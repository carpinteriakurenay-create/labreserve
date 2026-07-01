package com.labreserve.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.labreserve.dto.ApiResponse;
import com.labreserve.dto.ReviewCreateRequest;
import com.labreserve.dto.ReviewVO;
import com.labreserve.service.ReviewService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reviews")
public class ReviewController {

    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<IPage<ReviewVO>> list(
            @RequestParam(required = false) Long labId,
            @RequestParam(required = false) Long userId,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize) {
        IPage<ReviewVO> page = reviewService.listReviews(labId, userId, pageNum, pageSize);
        return ApiResponse.success(page);
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<ReviewVO>> create(@Valid @RequestBody ReviewCreateRequest request) {
        Long userId = getCurrentUserId();
        ReviewVO vo = reviewService.createReview(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("评价提交成功", vo));
    }

    @GetMapping("/booking/{bookingId}")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<ReviewVO> getByBookingId(@PathVariable Long bookingId) {
        Long userId = getCurrentUserId();
        ReviewVO vo = reviewService.getReviewByBookingId(bookingId, userId);
        return ApiResponse.success(vo);
    }

    @GetMapping("/lab/{labId}")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<IPage<ReviewVO>> listByLab(
            @PathVariable Long labId,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize) {
        IPage<ReviewVO> page = reviewService.listReviews(labId, null, pageNum, pageSize);
        return ApiResponse.success(page);
    }

    @DeleteMapping("/{reviewId}")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<Void> delete(@PathVariable Long reviewId) {
        Long userId = getCurrentUserId();
        reviewService.deleteReview(reviewId, userId);
        return ApiResponse.success("删除成功", null);
    }

    private Long getCurrentUserId() {
        return (Long) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();
    }
}
