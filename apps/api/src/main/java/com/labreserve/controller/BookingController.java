package com.labreserve.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.labreserve.dto.ApiResponse;
import com.labreserve.dto.ApprovalRequest;
import com.labreserve.dto.BookingCreateRequest;
import com.labreserve.dto.BookingUpdateRequest;
import com.labreserve.dto.BookingVO;
import com.labreserve.dto.TimeSlot;
import com.labreserve.service.BookingService;
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

import java.util.List;

@RestController
@RequestMapping("/api/bookings")
public class BookingController {

    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<IPage<BookingVO>> list(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long labId,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String dateFrom,
            @RequestParam(required = false) String dateTo,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize) {
        IPage<BookingVO> page = bookingService.listBookings(
                status, labId, userId, dateFrom, dateTo, pageNum, pageSize);
        return ApiResponse.success(page);
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<BookingVO>> create(@Valid @RequestBody BookingCreateRequest request) {
        Long userId = getCurrentUserId();
        BookingVO vo = bookingService.createBooking(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("预约提交成功", vo));
    }

    @GetMapping("/available-slots")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<List<TimeSlot>> getAvailableSlots(
            @RequestParam Long labId,
            @RequestParam String date) {
        List<TimeSlot> slots = bookingService.getAvailableSlots(labId, date);
        return ApiResponse.success(slots);
    }

    @GetMapping("/mine")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<IPage<BookingVO>> listMine(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize) {
        Long userId = getCurrentUserId();
        IPage<BookingVO> page = bookingService.listMyBookings(userId, status, pageNum, pageSize);
        return ApiResponse.success(page);
    }

    @GetMapping("/pending")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public ApiResponse<IPage<BookingVO>> listPending(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize) {
        IPage<BookingVO> page = bookingService.listPendingApprovals(pageNum, pageSize);
        return ApiResponse.success(page);
    }

    @GetMapping("/{bookingId}")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<BookingVO> getById(@PathVariable Long bookingId) {
        BookingVO vo = bookingService.getBookingById(bookingId);
        return ApiResponse.success(vo);
    }

    @PutMapping("/{bookingId}")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<BookingVO> update(
            @PathVariable Long bookingId,
            @Valid @RequestBody BookingUpdateRequest request) {
        Long userId = getCurrentUserId();
        BookingVO vo = bookingService.updateBooking(bookingId, request, userId);
        return ApiResponse.success("修改成功", vo);
    }

    @PutMapping("/{bookingId}/approve")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public ApiResponse<BookingVO> approve(
            @PathVariable Long bookingId,
            @Valid @RequestBody ApprovalRequest request) {
        Long approverId = getCurrentUserId();
        BookingVO vo = bookingService.approveBooking(
                bookingId, request.isApproved(), request.getRejectReason(), approverId);
        return ApiResponse.success(request.isApproved() ? "审批通过" : "已拒绝", vo);
    }

    @PutMapping("/{bookingId}/cancel")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<Void> cancel(@PathVariable Long bookingId) {
        Long userId = getCurrentUserId();
        bookingService.cancelBooking(bookingId, userId);
        return ApiResponse.success("取消成功", null);
    }

    @PutMapping("/{bookingId}/complete")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public ApiResponse<Void> complete(@PathVariable Long bookingId) {
        bookingService.completeBooking(bookingId);
        return ApiResponse.success("确认完成", null);
    }

    private Long getCurrentUserId() {
        return (Long) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();
    }
}
