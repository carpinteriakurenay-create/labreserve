package com.labreserve.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.labreserve.dto.BorrowCreateRequest;
import com.labreserve.dto.BorrowVO;
import com.labreserve.entity.Borrow;
import com.labreserve.entity.Equipment;
import com.labreserve.entity.User;
import com.labreserve.enums.BorrowStatus;
import com.labreserve.enums.EquipmentStatus;
import com.labreserve.exception.BusinessException;
import com.labreserve.mapper.BorrowMapper;
import com.labreserve.mapper.EquipmentMapper;
import com.labreserve.mapper.UserMapper;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class BorrowService {

    private final BorrowMapper borrowMapper;
    private final EquipmentMapper equipmentMapper;
    private final UserMapper userMapper;
    private final RedissonClient redissonClient;

    public BorrowService(BorrowMapper borrowMapper, EquipmentMapper equipmentMapper,
                         UserMapper userMapper, RedissonClient redissonClient) {
        this.borrowMapper = borrowMapper;
        this.equipmentMapper = equipmentMapper;
        this.userMapper = userMapper;
        this.redissonClient = redissonClient;
    }

    @Transactional
    public BorrowVO createBorrow(BorrowCreateRequest request, Long userId) {
        Equipment equipment = equipmentMapper.selectById(request.getEquipmentId());
        if (equipment == null) {
            throw new BusinessException("NOT_FOUND", "设备不存在");
        }
        if (equipment.getStatus() != EquipmentStatus.AVAILABLE) {
            throw new BusinessException("EQUIPMENT_UNAVAILABLE", "设备不可用，已被借出或维修中");
        }

        Borrow borrow = new Borrow();
        borrow.setEquipmentId(request.getEquipmentId());
        borrow.setUserId(userId);
        borrow.setBorrowDate(LocalDate.parse(request.getBorrowDate()));
        borrow.setExpectedReturn(LocalDate.parse(request.getExpectedReturn()));
        borrow.setPurpose(request.getPurpose());
        borrow.setStatus(BorrowStatus.PENDING);

        borrowMapper.insert(borrow);
        return toBorrowVO(borrow);
    }

    public BorrowVO getBorrowById(Long id) {
        Borrow borrow = borrowMapper.selectById(id);
        if (borrow == null) {
            throw new BusinessException("NOT_FOUND", "借用记录不存在");
        }
        BorrowVO vo = toBorrowVO(borrow);
        populateJoinedFields(Collections.singletonList(vo));
        return vo;
    }

    public IPage<BorrowVO> listBorrows(String statusStr, Long equipmentId, Long userId,
                                       int pageNum, int pageSize) {
        LambdaQueryWrapper<Borrow> wrapper = new LambdaQueryWrapper<>();

        if (statusStr != null && !statusStr.isBlank()) {
            try {
                wrapper.eq(Borrow::getStatus, BorrowStatus.valueOf(statusStr));
            } catch (IllegalArgumentException e) {
                throw new BusinessException("INVALID_STATUS", "无效的借用状态");
            }
        }
        if (equipmentId != null) {
            wrapper.eq(Borrow::getEquipmentId, equipmentId);
        }
        if (userId != null) {
            wrapper.eq(Borrow::getUserId, userId);
        }

        wrapper.orderByDesc(Borrow::getCreatedAt);

        Page<Borrow> page = new Page<>(pageNum, pageSize);
        IPage<Borrow> result = borrowMapper.selectPage(page, wrapper);
        IPage<BorrowVO> voPage = result.convert(this::toBorrowVO);
        populateJoinedFields(voPage.getRecords());
        return voPage;
    }

    public IPage<BorrowVO> listMyBorrows(Long userId, String statusStr, int pageNum, int pageSize) {
        LambdaQueryWrapper<Borrow> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Borrow::getUserId, userId);

        if (statusStr != null && !statusStr.isBlank()) {
            try {
                wrapper.eq(Borrow::getStatus, BorrowStatus.valueOf(statusStr));
            } catch (IllegalArgumentException e) {
                throw new BusinessException("INVALID_STATUS", "无效的借用状态");
            }
        }

        wrapper.orderByDesc(Borrow::getCreatedAt);

        Page<Borrow> page = new Page<>(pageNum, pageSize);
        IPage<Borrow> result = borrowMapper.selectPage(page, wrapper);
        IPage<BorrowVO> voPage = result.convert(this::toBorrowVO);
        populateJoinedFields(voPage.getRecords());
        return voPage;
    }

    @Transactional
    public BorrowVO approveBorrow(Long id, boolean approved, String rejectReason, Long approverId) {
        String lockKey = "borrow:lock:" + id;
        RLock lock = redissonClient.getLock(lockKey);
        boolean locked = false;
        try {
            locked = lock.tryLock(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException("LOCK_FAILED", "系统繁忙，请稍后重试");
        }
        if (!locked) {
            throw new BusinessException("LOCK_FAILED", "该借用申请正在被其他管理员处理，请稍后重试");
        }
        try {
            Borrow borrow = borrowMapper.selectById(id);
            if (borrow == null) {
                throw new BusinessException("NOT_FOUND", "借用记录不存在");
            }
            if (borrow.getStatus() != BorrowStatus.PENDING) {
                throw new BusinessException("ALREADY_PROCESSED", "该借用申请已被处理");
            }

            LambdaUpdateWrapper<Borrow> wrapper = new LambdaUpdateWrapper<>();
            wrapper.eq(Borrow::getId, id);

            if (approved) {
                wrapper.set(Borrow::getStatus, BorrowStatus.BORROWING);
                wrapper.set(Borrow::getApproverId, approverId);

                LambdaUpdateWrapper<Equipment> equipmentWrapper = new LambdaUpdateWrapper<>();
                equipmentWrapper.eq(Equipment::getId, borrow.getEquipmentId())
                        .set(Equipment::getStatus, EquipmentStatus.BORROWED);
                equipmentMapper.update(equipmentWrapper);
            } else {
                if (rejectReason == null || rejectReason.isBlank()) {
                    throw new BusinessException("REJECT_REASON_REQUIRED", "拒绝时必须填写原因");
                }
                wrapper.set(Borrow::getStatus, BorrowStatus.REJECTED);
                wrapper.set(Borrow::getRejectReason, rejectReason);
                wrapper.set(Borrow::getApproverId, approverId);
            }

            borrowMapper.update(wrapper);

            Borrow updated = borrowMapper.selectById(id);
            BorrowVO vo = toBorrowVO(updated);
            populateJoinedFields(Collections.singletonList(vo));
            return vo;
        } finally {
            lock.unlock();
        }
    }

    @Transactional
    public void returnBorrow(Long id, String actualReturnStr) {
        Borrow borrow = borrowMapper.selectById(id);
        if (borrow == null) {
            throw new BusinessException("NOT_FOUND", "借用记录不存在");
        }
        if (borrow.getStatus() != BorrowStatus.BORROWING) {
            throw new BusinessException("INVALID_STATUS", "只有借用中的记录可以归还");
        }

        LocalDate actualReturn = actualReturnStr != null && !actualReturnStr.isBlank()
                ? LocalDate.parse(actualReturnStr)
                : LocalDate.now();

        LambdaUpdateWrapper<Borrow> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(Borrow::getId, id)
                .set(Borrow::getStatus, BorrowStatus.RETURNED)
                .set(Borrow::getActualReturn, actualReturn);
        borrowMapper.update(wrapper);

        LambdaUpdateWrapper<Equipment> equipmentWrapper = new LambdaUpdateWrapper<>();
        equipmentWrapper.eq(Equipment::getId, borrow.getEquipmentId())
                .set(Equipment::getStatus, EquipmentStatus.AVAILABLE);
        equipmentMapper.update(equipmentWrapper);
    }

    private BorrowVO toBorrowVO(Borrow borrow) {
        return BorrowVO.builder()
                .id(borrow.getId())
                .equipmentId(borrow.getEquipmentId())
                .userId(borrow.getUserId())
                .borrowDate(borrow.getBorrowDate())
                .expectedReturn(borrow.getExpectedReturn())
                .actualReturn(borrow.getActualReturn())
                .purpose(borrow.getPurpose())
                .status(borrow.getStatus().name())
                .rejectReason(borrow.getRejectReason())
                .approverId(borrow.getApproverId())
                .createdAt(borrow.getCreatedAt())
                .updatedAt(borrow.getUpdatedAt())
                .build();
    }

    private void populateJoinedFields(List<BorrowVO> vos) {
        if (vos.isEmpty()) {
            return;
        }

        Set<Long> equipmentIds = vos.stream()
                .map(BorrowVO::getEquipmentId)
                .collect(Collectors.toSet());
        Set<Long> userIds = vos.stream()
                .map(BorrowVO::getUserId)
                .collect(Collectors.toSet());
        Set<Long> approverIds = vos.stream()
                .map(BorrowVO::getApproverId)
                .filter(id -> id != null)
                .collect(Collectors.toSet());

        Map<Long, String> equipmentIdToName = Collections.emptyMap();
        if (!equipmentIds.isEmpty()) {
            List<Equipment> equipments = equipmentMapper.selectBatchIds(equipmentIds);
            equipmentIdToName = equipments.stream()
                    .collect(Collectors.toMap(Equipment::getId, Equipment::getName));
        }

        Map<Long, String> userIdToName = Collections.emptyMap();
        Set<Long> allUserIds = new java.util.HashSet<>(userIds);
        allUserIds.addAll(approverIds);
        if (!allUserIds.isEmpty()) {
            List<User> users = userMapper.selectBatchIds(allUserIds);
            userIdToName = users.stream()
                    .collect(Collectors.toMap(User::getId, User::getRealName));
        }

        for (BorrowVO vo : vos) {
            vo.setEquipmentName(equipmentIdToName.get(vo.getEquipmentId()));
            vo.setUserName(userIdToName.get(vo.getUserId()));
            if (vo.getApproverId() != null) {
                vo.setApproverName(userIdToName.get(vo.getApproverId()));
            }
        }
    }
}
