package com.labreserve.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.labreserve.dto.RepairLogCreateRequest;
import com.labreserve.dto.RepairLogUpdateRequest;
import com.labreserve.dto.RepairLogVO;
import com.labreserve.entity.Equipment;
import com.labreserve.entity.RepairLog;
import com.labreserve.entity.User;
import com.labreserve.enums.RepairStatus;
import com.labreserve.exception.BusinessException;
import com.labreserve.mapper.EquipmentMapper;
import com.labreserve.mapper.RepairLogMapper;
import com.labreserve.mapper.UserMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class RepairLogService {

    private final RepairLogMapper repairLogMapper;
    private final EquipmentMapper equipmentMapper;
    private final UserMapper userMapper;

    public RepairLogService(RepairLogMapper repairLogMapper,
                            EquipmentMapper equipmentMapper,
                            UserMapper userMapper) {
        this.repairLogMapper = repairLogMapper;
        this.equipmentMapper = equipmentMapper;
        this.userMapper = userMapper;
    }

    public IPage<RepairLogVO> listRepairLogs(Long equipmentId, String status,
                                              int pageNum, int pageSize) {
        LambdaQueryWrapper<RepairLog> wrapper = new LambdaQueryWrapper<>();
        if (equipmentId != null) {
            wrapper.eq(RepairLog::getEquipmentId, equipmentId);
        }
        if (status != null && !status.isBlank()) {
            wrapper.eq(RepairLog::getStatus, RepairStatus.valueOf(status));
        }
        wrapper.orderByDesc(RepairLog::getCreatedAt);

        Page<RepairLog> page = new Page<>(pageNum, pageSize);
        IPage<RepairLog> result = repairLogMapper.selectPage(page, wrapper);
        IPage<RepairLogVO> voPage = result.convert(this::toRepairLogVO);
        populateJoinedFields(voPage.getRecords());
        return voPage;
    }

    public RepairLogVO getRepairLogById(Long id) {
        RepairLog log = repairLogMapper.selectById(id);
        if (log == null) {
            throw new BusinessException("NOT_FOUND", "报修记录不存在");
        }
        RepairLogVO vo = toRepairLogVO(log);
        populateJoinedFields(Collections.singletonList(vo));
        return vo;
    }

    @Transactional
    public RepairLogVO createRepairLog(RepairLogCreateRequest request, Long reporterId) {
        Equipment equipment = equipmentMapper.selectById(request.getEquipmentId());
        if (equipment == null) {
            throw new BusinessException("NOT_FOUND", "设备不存在");
        }

        RepairLog log = new RepairLog();
        log.setEquipmentId(request.getEquipmentId());
        log.setReporterId(reporterId);
        log.setDescription(request.getDescription());
        log.setStatus(RepairStatus.PENDING);
        repairLogMapper.insert(log);

        return toRepairLogVO(log);
    }

    @Transactional
    public RepairLogVO updateRepairLog(Long id, RepairLogUpdateRequest request, Long userId) {
        RepairLog log = repairLogMapper.selectById(id);
        if (log == null) {
            throw new BusinessException("NOT_FOUND", "报修记录不存在");
        }

        LambdaUpdateWrapper<RepairLog> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(RepairLog::getId, id);

        if (request.getDescription() != null) {
            wrapper.set(RepairLog::getDescription, request.getDescription());
        }
        if (request.getStatus() != null) {
            wrapper.set(RepairLog::getStatus, request.getStatus());
        }

        repairLogMapper.update(wrapper);

        RepairLog updated = repairLogMapper.selectById(id);
        RepairLogVO vo = toRepairLogVO(updated);
        populateJoinedFields(Collections.singletonList(vo));
        return vo;
    }

    @Transactional
    public RepairLogVO updateStatus(Long id, String statusStr) {
        RepairLog log = repairLogMapper.selectById(id);
        if (log == null) {
            throw new BusinessException("NOT_FOUND", "报修记录不存在");
        }

        RepairStatus status;
        try {
            status = RepairStatus.valueOf(statusStr);
        } catch (IllegalArgumentException e) {
            throw new BusinessException("INVALID_STATUS", "无效的报修状态");
        }

        LambdaUpdateWrapper<RepairLog> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(RepairLog::getId, id).set(RepairLog::getStatus, status);
        repairLogMapper.update(wrapper);

        RepairLog updated = repairLogMapper.selectById(id);
        RepairLogVO vo = toRepairLogVO(updated);
        populateJoinedFields(Collections.singletonList(vo));
        return vo;
    }

    private RepairLogVO toRepairLogVO(RepairLog log) {
        return RepairLogVO.builder()
                .id(log.getId())
                .equipmentId(log.getEquipmentId())
                .reporterId(log.getReporterId())
                .description(log.getDescription())
                .status(log.getStatus().name())
                .createdAt(log.getCreatedAt())
                .updatedAt(log.getUpdatedAt())
                .build();
    }

    private void populateJoinedFields(List<RepairLogVO> vos) {
        Set<Long> equipmentIds = vos.stream()
                .map(RepairLogVO::getEquipmentId)
                .filter(id -> id != null)
                .collect(Collectors.toSet());
        Set<Long> reporterIds = vos.stream()
                .map(RepairLogVO::getReporterId)
                .filter(id -> id != null)
                .collect(Collectors.toSet());

        Map<Long, String> equipIdToName = Collections.emptyMap();
        if (!equipmentIds.isEmpty()) {
            List<Equipment> equipments = equipmentMapper.selectBatchIds(equipmentIds);
            equipIdToName = equipments.stream()
                    .collect(Collectors.toMap(Equipment::getId, Equipment::getName));
        }

        Map<Long, String> userIdToName = Collections.emptyMap();
        if (!reporterIds.isEmpty()) {
            List<User> users = userMapper.selectBatchIds(reporterIds);
            userIdToName = users.stream()
                    .collect(Collectors.toMap(User::getId, User::getRealName));
        }

        for (RepairLogVO vo : vos) {
            if (vo.getEquipmentId() != null) {
                vo.setEquipmentName(equipIdToName.get(vo.getEquipmentId()));
            }
            if (vo.getReporterId() != null) {
                vo.setReporterName(userIdToName.get(vo.getReporterId()));
            }
        }
    }
}
