package com.labreserve.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.labreserve.dto.EquipmentCreateRequest;
import com.labreserve.dto.EquipmentUpdateRequest;
import com.labreserve.dto.EquipmentVO;
import com.labreserve.entity.Equipment;
import com.labreserve.entity.Lab;
import com.labreserve.enums.EquipmentStatus;
import com.labreserve.exception.BusinessException;
import com.labreserve.mapper.EquipmentMapper;
import com.labreserve.mapper.LabMapper;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class EquipmentService {

    private final EquipmentMapper equipmentMapper;
    private final LabMapper labMapper;

    public EquipmentService(EquipmentMapper equipmentMapper, LabMapper labMapper) {
        this.equipmentMapper = equipmentMapper;
        this.labMapper = labMapper;
    }

    public IPage<EquipmentVO> listEquipment(Long labId, EquipmentStatus status, String name,
                                            int pageNum, int pageSize) {
        LambdaQueryWrapper<Equipment> wrapper = new LambdaQueryWrapper<>();
        if (labId != null) {
            wrapper.eq(Equipment::getLabId, labId);
        }
        if (status != null) {
            wrapper.eq(Equipment::getStatus, status);
        }
        if (name != null && !name.isBlank()) {
            wrapper.like(Equipment::getName, name);
        }
        wrapper.orderByDesc(Equipment::getCreatedAt);

        Page<Equipment> page = new Page<>(pageNum, pageSize);
        IPage<Equipment> result = equipmentMapper.selectPage(page, wrapper);
        IPage<EquipmentVO> voPage = result.convert(this::toEquipmentVO);
        populateLabNames(voPage.getRecords());
        return voPage;
    }

    public EquipmentVO getEquipmentById(Long id) {
        Equipment equipment = equipmentMapper.selectById(id);
        if (equipment == null) {
            throw new BusinessException("NOT_FOUND", "设备不存在");
        }
        EquipmentVO vo = toEquipmentVO(equipment);
        populateLabNames(Collections.singletonList(vo));
        return vo;
    }

    @Transactional
    public EquipmentVO createEquipment(EquipmentCreateRequest request) {
        Long count = equipmentMapper.selectCount(
                new LambdaQueryWrapper<Equipment>().eq(Equipment::getSerialNumber, request.getSerialNumber())
        );
        if (count > 0) {
            throw new BusinessException("SERIAL_NUMBER_EXISTS", "设备序列号已存在");
        }

        Equipment equipment = new Equipment();
        equipment.setLabId(request.getLabId());
        equipment.setName(request.getName());
        equipment.setModel(request.getModel());
        equipment.setSerialNumber(request.getSerialNumber());
        equipment.setDescription(request.getDescription());
        equipment.setStatus(EquipmentStatus.AVAILABLE);

        try {
            equipmentMapper.insert(equipment);
        } catch (DuplicateKeyException e) {
            throw new BusinessException("SERIAL_NUMBER_EXISTS", "设备序列号已存在");
        }
        return toEquipmentVO(equipment);
    }

    @Transactional
    public EquipmentVO updateEquipment(Long id, EquipmentUpdateRequest request) {
        Equipment equipment = equipmentMapper.selectById(id);
        if (equipment == null) {
            throw new BusinessException("NOT_FOUND", "设备不存在");
        }

        if (request.getSerialNumber() != null) {
            Long count = equipmentMapper.selectCount(
                    new LambdaQueryWrapper<Equipment>()
                            .eq(Equipment::getSerialNumber, request.getSerialNumber())
                            .ne(Equipment::getId, id)
            );
            if (count > 0) {
                throw new BusinessException("SERIAL_NUMBER_EXISTS", "设备序列号已存在");
            }
        }

        LambdaUpdateWrapper<Equipment> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(Equipment::getId, id);

        if (request.getLabId() != null) {
            wrapper.set(Equipment::getLabId, request.getLabId());
        }
        if (request.getName() != null) {
            wrapper.set(Equipment::getName, request.getName());
        }
        if (request.getModel() != null) {
            wrapper.set(Equipment::getModel, request.getModel());
        }
        if (request.getSerialNumber() != null) {
            wrapper.set(Equipment::getSerialNumber, request.getSerialNumber());
        }
        if (request.getDescription() != null) {
            wrapper.set(Equipment::getDescription, request.getDescription());
        }
        if (request.getStatus() != null) {
            wrapper.set(Equipment::getStatus, request.getStatus());
        }

        try {
            equipmentMapper.update(wrapper);
        } catch (DuplicateKeyException e) {
            throw new BusinessException("SERIAL_NUMBER_EXISTS", "设备序列号已存在");
        }

        Equipment updated = equipmentMapper.selectById(id);
        return toEquipmentVO(updated);
    }

    @Transactional
    public void deleteEquipment(Long id) {
        Equipment equipment = equipmentMapper.selectById(id);
        if (equipment == null) {
            throw new BusinessException("NOT_FOUND", "设备不存在");
        }
        equipmentMapper.deleteById(id);
    }

    @Transactional
    public void updateStatus(Long id, EquipmentStatus status) {
        Equipment equipment = equipmentMapper.selectById(id);
        if (equipment == null) {
            throw new BusinessException("NOT_FOUND", "设备不存在");
        }

        LambdaUpdateWrapper<Equipment> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(Equipment::getId, id).set(Equipment::getStatus, status);
        equipmentMapper.update(wrapper);
    }

    private void populateLabNames(List<EquipmentVO> vos) {
        Set<Long> labIds = vos.stream()
                .map(EquipmentVO::getLabId)
                .filter(id -> id != null)
                .collect(Collectors.toSet());

        if (labIds.isEmpty()) {
            return;
        }

        List<Lab> labs = labMapper.selectBatchIds(labIds);
        Map<Long, String> idToName = labs.stream()
                .collect(Collectors.toMap(Lab::getId, Lab::getName));

        for (EquipmentVO vo : vos) {
            if (vo.getLabId() != null) {
                vo.setLabName(idToName.get(vo.getLabId()));
            }
        }
    }

    private EquipmentVO toEquipmentVO(Equipment equipment) {
        return EquipmentVO.builder()
                .id(equipment.getId())
                .labId(equipment.getLabId())
                .name(equipment.getName())
                .model(equipment.getModel())
                .serialNumber(equipment.getSerialNumber())
                .description(equipment.getDescription())
                .status(equipment.getStatus().name())
                .createdAt(equipment.getCreatedAt())
                .updatedAt(equipment.getUpdatedAt())
                .build();
    }
}
