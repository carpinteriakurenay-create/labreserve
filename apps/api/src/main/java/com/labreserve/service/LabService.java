package com.labreserve.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.labreserve.dto.LabCreateRequest;
import com.labreserve.dto.LabHoursBatchRequest;
import com.labreserve.dto.LabHoursVO;
import com.labreserve.dto.LabUpdateRequest;
import com.labreserve.dto.LabVO;
import com.labreserve.entity.Lab;
import com.labreserve.entity.LabHours;
import com.labreserve.entity.User;
import com.labreserve.enums.LabStatus;
import com.labreserve.exception.BusinessException;
import com.labreserve.mapper.LabHoursMapper;
import com.labreserve.mapper.LabMapper;
import com.labreserve.mapper.UserMapper;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class LabService {

    private final LabMapper labMapper;
    private final LabHoursMapper labHoursMapper;
    private final UserMapper userMapper;

    public LabService(LabMapper labMapper, LabHoursMapper labHoursMapper, UserMapper userMapper) {
        this.labMapper = labMapper;
        this.labHoursMapper = labHoursMapper;
        this.userMapper = userMapper;
    }

    public IPage<LabVO> listLabs(String name, LabStatus status, int pageNum, int pageSize) {
        LambdaQueryWrapper<Lab> wrapper = new LambdaQueryWrapper<>();
        if (name != null && !name.isBlank()) {
            wrapper.like(Lab::getName, name);
        }
        if (status != null) {
            wrapper.eq(Lab::getStatus, status);
        }
        wrapper.orderByDesc(Lab::getCreatedAt);

        Page<Lab> page = new Page<>(pageNum, pageSize);
        IPage<Lab> result = labMapper.selectPage(page, wrapper);
        IPage<LabVO> voPage = result.convert(this::toLabVO);
        populateManagerNames(voPage.getRecords());
        return voPage;
    }

    @Cacheable(value = "lab", key = "#id")
    public LabVO getLabById(Long id) {
        Lab lab = labMapper.selectById(id);
        if (lab == null) {
            throw new BusinessException("NOT_FOUND", "实验室不存在");
        }
        LabVO vo = toLabVO(lab);
        populateManagerNames(Collections.singletonList(vo));
        return vo;
    }

    @Transactional
    @CacheEvict(value = "lab", allEntries = true)
    public LabVO createLab(LabCreateRequest request) {
        Long count = labMapper.selectCount(
                new LambdaQueryWrapper<Lab>().eq(Lab::getName, request.getName())
        );
        if (count > 0) {
            throw new BusinessException("NAME_EXISTS", "实验室名称已存在");
        }

        Lab lab = new Lab();
        lab.setName(request.getName());
        lab.setLocation(request.getLocation());
        lab.setCapacity(request.getCapacity());
        lab.setDescription(request.getDescription());
        lab.setImageUrl(request.getImageUrl());
        lab.setManagerId(request.getManagerId());
        lab.setStatus(LabStatus.AVAILABLE);
        lab.setEquipmentNum(0);

        labMapper.insert(lab);
        return toLabVO(lab);
    }

    @Transactional
    @CacheEvict(value = "lab", key = "#id")
    public LabVO updateLab(Long id, LabUpdateRequest request) {
        Lab lab = labMapper.selectById(id);
        if (lab == null) {
            throw new BusinessException("NOT_FOUND", "实验室不存在");
        }

        if (request.getName() != null) {
            Long count = labMapper.selectCount(
                    new LambdaQueryWrapper<Lab>()
                            .eq(Lab::getName, request.getName())
                            .ne(Lab::getId, id)
            );
            if (count > 0) {
                throw new BusinessException("NAME_EXISTS", "实验室名称已存在");
            }
        }

        LambdaUpdateWrapper<Lab> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(Lab::getId, id);

        if (request.getName() != null) {
            wrapper.set(Lab::getName, request.getName());
        }
        if (request.getLocation() != null) {
            wrapper.set(Lab::getLocation, request.getLocation());
        }
        if (request.getCapacity() != null) {
            wrapper.set(Lab::getCapacity, request.getCapacity());
        }
        if (request.getDescription() != null) {
            wrapper.set(Lab::getDescription, request.getDescription());
        }
        if (request.getImageUrl() != null) {
            wrapper.set(Lab::getImageUrl, request.getImageUrl());
        }
        if (request.getStatus() != null) {
            wrapper.set(Lab::getStatus, request.getStatus());
        }
        if (request.getManagerId() != null) {
            wrapper.set(Lab::getManagerId, request.getManagerId());
        }

        labMapper.update(wrapper);

        Lab updated = labMapper.selectById(id);
        return toLabVO(updated);
    }

    @Transactional
    @CacheEvict(value = "lab", key = "#id")
    public void deleteLab(Long id) {
        Lab lab = labMapper.selectById(id);
        if (lab == null) {
            throw new BusinessException("NOT_FOUND", "实验室不存在");
        }
        labMapper.deleteById(id);
    }

    @Transactional
    @CacheEvict(value = "lab", key = "#id")
    public void toggleStatus(Long id) {
        Lab lab = labMapper.selectById(id);
        if (lab == null) {
            throw new BusinessException("NOT_FOUND", "实验室不存在");
        }

        LabStatus newStatus = lab.getStatus() == LabStatus.AVAILABLE
                ? LabStatus.CLOSED
                : LabStatus.AVAILABLE;

        LambdaUpdateWrapper<Lab> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(Lab::getId, id).set(Lab::getStatus, newStatus);
        labMapper.update(wrapper);
    }

    @Cacheable(value = "labHours", key = "#labId")
    public List<LabHoursVO> getLabHours(Long labId) {
        Lab lab = labMapper.selectById(labId);
        if (lab == null) {
            throw new BusinessException("NOT_FOUND", "实验室不存在");
        }

        LambdaQueryWrapper<LabHours> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(LabHours::getLabId, labId)
                .orderByAsc(LabHours::getDayOfWeek, LabHours::getOpenTime);

        List<LabHours> hours = labHoursMapper.selectList(wrapper);
        return hours.stream().map(this::toLabHoursVO).collect(Collectors.toList());
    }

    @Transactional
    @CacheEvict(value = "labHours", key = "#labId")
    public void batchReplaceLabHours(Long labId, List<LabHoursBatchRequest.LabHoursItem> items) {
        Lab lab = labMapper.selectById(labId);
        if (lab == null) {
            throw new BusinessException("NOT_FOUND", "实验室不存在");
        }

        labHoursMapper.hardDeleteByLabId(labId);

        for (LabHoursBatchRequest.LabHoursItem item : items) {
            LabHours hours = new LabHours();
            hours.setLabId(labId);
            hours.setDayOfWeek(item.getDayOfWeek());
            hours.setOpenTime(item.getOpenTime());
            hours.setCloseTime(item.getCloseTime());
            labHoursMapper.insert(hours);
        }
    }

    private void populateManagerNames(List<LabVO> vos) {
        Set<Long> managerIds = vos.stream()
                .map(LabVO::getManagerId)
                .filter(id -> id != null)
                .collect(Collectors.toSet());

        if (managerIds.isEmpty()) {
            return;
        }

        List<User> managers = userMapper.selectBatchIds(managerIds);
        Map<Long, String> idToName = managers.stream()
                .collect(Collectors.toMap(User::getId, User::getRealName));

        for (LabVO vo : vos) {
            if (vo.getManagerId() != null) {
                vo.setManagerName(idToName.get(vo.getManagerId()));
            }
        }
    }

    private LabVO toLabVO(Lab lab) {
        return LabVO.builder()
                .id(lab.getId())
                .name(lab.getName())
                .location(lab.getLocation())
                .capacity(lab.getCapacity())
                .description(lab.getDescription())
                .imageUrl(lab.getImageUrl())
                .equipmentNum(lab.getEquipmentNum())
                .status(lab.getStatus().name())
                .managerId(lab.getManagerId())
                .createdAt(lab.getCreatedAt())
                .updatedAt(lab.getUpdatedAt())
                .build();
    }

    private LabHoursVO toLabHoursVO(LabHours hours) {
        return LabHoursVO.builder()
                .id(hours.getId())
                .labId(hours.getLabId())
                .dayOfWeek(hours.getDayOfWeek())
                .openTime(hours.getOpenTime())
                .closeTime(hours.getCloseTime())
                .build();
    }
}
