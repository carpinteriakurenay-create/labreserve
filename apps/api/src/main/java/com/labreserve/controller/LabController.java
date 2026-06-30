package com.labreserve.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.labreserve.dto.ApiResponse;
import com.labreserve.dto.LabCreateRequest;
import com.labreserve.dto.LabHoursBatchRequest;
import com.labreserve.dto.LabHoursVO;
import com.labreserve.dto.LabUpdateRequest;
import com.labreserve.dto.LabVO;
import com.labreserve.enums.LabStatus;
import com.labreserve.exception.BusinessException;
import com.labreserve.service.LabService;
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

import java.util.List;

@RestController
@RequestMapping("/api/labs")
public class LabController {

    private final LabService labService;

    public LabController(LabService labService) {
        this.labService = labService;
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<IPage<LabVO>> list(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize) {
        LabStatus labStatus = null;
        if (status != null) {
            try {
                labStatus = LabStatus.valueOf(status);
            } catch (IllegalArgumentException e) {
                throw new BusinessException("INVALID_STATUS", "无效的实验室状态");
            }
        }
        IPage<LabVO> page = labService.listLabs(name, labStatus, pageNum, pageSize);
        return ApiResponse.success(page);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<LabVO>> create(@Valid @RequestBody LabCreateRequest request) {
        LabVO vo = labService.createLab(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("创建成功", vo));
    }

    @GetMapping("/{labId}")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<LabVO> getById(@PathVariable Long labId) {
        LabVO vo = labService.getLabById(labId);
        return ApiResponse.success(vo);
    }

    @PutMapping("/{labId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<LabVO> update(
            @PathVariable Long labId,
            @Valid @RequestBody LabUpdateRequest request) {
        LabVO vo = labService.updateLab(labId, request);
        return ApiResponse.success("更新成功", vo);
    }

    @DeleteMapping("/{labId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> delete(@PathVariable Long labId) {
        labService.deleteLab(labId);
        return ApiResponse.success("删除成功", null);
    }

    @PutMapping("/{labId}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> toggleStatus(@PathVariable Long labId) {
        labService.toggleStatus(labId);
        return ApiResponse.success(null);
    }

    @GetMapping("/{labId}/hours")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<List<LabHoursVO>> getHours(@PathVariable Long labId) {
        List<LabHoursVO> hours = labService.getLabHours(labId);
        return ApiResponse.success(hours);
    }

    @PutMapping("/{labId}/hours")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> replaceHours(
            @PathVariable Long labId,
            @Valid @RequestBody LabHoursBatchRequest request) {
        labService.batchReplaceLabHours(labId, request.getHours());
        return ApiResponse.success("更新成功", null);
    }
}
