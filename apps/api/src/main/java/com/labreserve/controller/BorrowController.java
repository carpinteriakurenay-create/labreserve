package com.labreserve.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.labreserve.dto.ApprovalRequest;
import com.labreserve.dto.ApiResponse;
import com.labreserve.dto.BorrowCreateRequest;
import com.labreserve.dto.BorrowVO;
import com.labreserve.service.BorrowService;
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

import java.util.Map;

@RestController
@RequestMapping("/api/borrows")
public class BorrowController {

    private final BorrowService borrowService;

    public BorrowController(BorrowService borrowService) {
        this.borrowService = borrowService;
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<IPage<BorrowVO>> list(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long equipmentId,
            @RequestParam(required = false) Long userId,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize) {
        IPage<BorrowVO> page = borrowService.listBorrows(
                status, equipmentId, userId, pageNum, pageSize);
        return ApiResponse.success(page);
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<BorrowVO>> create(
            @Valid @RequestBody BorrowCreateRequest request) {
        Long userId = getCurrentUserId();
        BorrowVO vo = borrowService.createBorrow(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("借用申请提交成功", vo));
    }

    @GetMapping("/mine")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<IPage<BorrowVO>> listMine(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize) {
        Long userId = getCurrentUserId();
        IPage<BorrowVO> page = borrowService.listMyBorrows(userId, status, pageNum, pageSize);
        return ApiResponse.success(page);
    }

    @GetMapping("/{borrowId}")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<BorrowVO> getById(@PathVariable Long borrowId) {
        BorrowVO vo = borrowService.getBorrowById(borrowId);
        return ApiResponse.success(vo);
    }

    @PutMapping("/{borrowId}/approve")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public ApiResponse<BorrowVO> approve(
            @PathVariable Long borrowId,
            @Valid @RequestBody ApprovalRequest request) {
        Long approverId = getCurrentUserId();
        BorrowVO vo = borrowService.approveBorrow(
                borrowId, request.isApproved(), request.getRejectReason(), approverId);
        return ApiResponse.success(request.isApproved() ? "审批通过" : "已拒绝", vo);
    }

    @PutMapping("/{borrowId}/return")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public ApiResponse<Void> returnBorrow(
            @PathVariable Long borrowId,
            @RequestBody(required = false) Map<String, String> body) {
        String actualReturn = body != null ? body.get("actualReturn") : null;
        borrowService.returnBorrow(borrowId, actualReturn);
        return ApiResponse.success("归还成功", null);
    }

    private Long getCurrentUserId() {
        return (Long) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();
    }
}
