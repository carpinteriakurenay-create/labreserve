package com.labreserve.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.labreserve.dto.ApiResponse;
import com.labreserve.dto.RepairLogCreateRequest;
import com.labreserve.dto.RepairLogUpdateRequest;
import com.labreserve.dto.RepairLogVO;
import com.labreserve.service.RepairLogService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/repair-logs")
public class RepairLogController {

    private final RepairLogService repairLogService;

    public RepairLogController(RepairLogService repairLogService) {
        this.repairLogService = repairLogService;
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<IPage<RepairLogVO>> list(
            @RequestParam(required = false) Long equipmentId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize) {
        IPage<RepairLogVO> page = repairLogService.listRepairLogs(
                equipmentId, status, pageNum, pageSize);
        return ApiResponse.success(page);
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<RepairLogVO>> create(
            @Valid @RequestBody RepairLogCreateRequest request) {
        Long reporterId = getCurrentUserId();
        RepairLogVO vo = repairLogService.createRepairLog(request, reporterId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("报修提交成功", vo));
    }

    @GetMapping("/{repairLogId}")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<RepairLogVO> getById(@PathVariable Long repairLogId) {
        RepairLogVO vo = repairLogService.getRepairLogById(repairLogId);
        return ApiResponse.success(vo);
    }

    @PutMapping("/{repairLogId}")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<RepairLogVO> update(
            @PathVariable Long repairLogId,
            @Valid @RequestBody RepairLogUpdateRequest request) {
        Long userId = getCurrentUserId();
        RepairLogVO vo = repairLogService.updateRepairLog(repairLogId, request, userId);
        return ApiResponse.success("更新成功", vo);
    }

    @PutMapping("/{repairLogId}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<RepairLogVO> updateStatus(
            @PathVariable Long repairLogId,
            @RequestBody RepairLogUpdateRequest request) {
        RepairLogVO vo = repairLogService.updateStatus(repairLogId,
                request.getStatus() != null ? request.getStatus().name() : null);
        return ApiResponse.success("状态更新成功", vo);
    }

    private Long getCurrentUserId() {
        return (Long) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();
    }
}
