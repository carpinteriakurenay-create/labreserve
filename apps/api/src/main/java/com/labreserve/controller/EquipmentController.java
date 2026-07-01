package com.labreserve.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.labreserve.dto.ApiResponse;
import com.labreserve.dto.EquipmentCreateRequest;
import com.labreserve.dto.EquipmentUpdateRequest;
import com.labreserve.dto.EquipmentVO;
import com.labreserve.enums.EquipmentStatus;
import com.labreserve.exception.BusinessException;
import com.labreserve.service.EquipmentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/equipment")
public class EquipmentController {

    private final EquipmentService equipmentService;

    public EquipmentController(EquipmentService equipmentService) {
        this.equipmentService = equipmentService;
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<IPage<EquipmentVO>> list(
            @RequestParam(required = false) Long labId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String name,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize) {
        EquipmentStatus equipmentStatus = null;
        if (status != null && !status.isBlank()) {
            try {
                equipmentStatus = EquipmentStatus.valueOf(status);
            } catch (IllegalArgumentException e) {
                throw new BusinessException("INVALID_STATUS", "无效的设备状态");
            }
        }
        IPage<EquipmentVO> page = equipmentService.listEquipment(
                labId, equipmentStatus, name, pageNum, pageSize);
        return ApiResponse.success(page);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<EquipmentVO>> create(
            @Valid @RequestBody EquipmentCreateRequest request) {
        EquipmentVO vo = equipmentService.createEquipment(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("创建成功", vo));
    }

    @GetMapping("/{equipmentId}")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<EquipmentVO> getById(@PathVariable Long equipmentId) {
        EquipmentVO vo = equipmentService.getEquipmentById(equipmentId);
        return ApiResponse.success(vo);
    }

    @PutMapping("/{equipmentId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<EquipmentVO> update(
            @PathVariable Long equipmentId,
            @Valid @RequestBody EquipmentUpdateRequest request) {
        EquipmentVO vo = equipmentService.updateEquipment(equipmentId, request);
        return ApiResponse.success("更新成功", vo);
    }

    @DeleteMapping("/{equipmentId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> delete(@PathVariable Long equipmentId) {
        equipmentService.deleteEquipment(equipmentId);
        return ApiResponse.success("删除成功", null);
    }

    @PutMapping("/{equipmentId}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> updateStatus(
            @PathVariable Long equipmentId,
            @RequestParam EquipmentStatus status) {
        equipmentService.updateStatus(equipmentId, status);
        return ApiResponse.success(null);
    }
}
