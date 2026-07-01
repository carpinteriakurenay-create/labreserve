package com.labreserve.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.labreserve.dto.ApiResponse;
import com.labreserve.dto.NoticeCreateRequest;
import com.labreserve.dto.NoticeUpdateRequest;
import com.labreserve.dto.NoticeVO;
import com.labreserve.service.NoticeService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
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
@RequestMapping("/api/notices")
public class NoticeController {

    private final NoticeService noticeService;

    public NoticeController(NoticeService noticeService) {
        this.noticeService = noticeService;
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<IPage<NoticeVO>> list(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String priority,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize) {
        IPage<NoticeVO> page = noticeService.listNotices(type, priority, pageNum, pageSize);
        return ApiResponse.success(page);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public ResponseEntity<ApiResponse<NoticeVO>> create(
            @Valid @RequestBody NoticeCreateRequest request) {
        Long publisherId = getCurrentUserId();
        NoticeVO vo = noticeService.createNotice(request, publisherId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("发布成功", vo));
    }

    @GetMapping("/{noticeId}")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<NoticeVO> getById(@PathVariable Long noticeId) {
        NoticeVO vo = noticeService.getNoticeById(noticeId);
        return ApiResponse.success(vo);
    }

    @PutMapping("/{noticeId}")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public ApiResponse<NoticeVO> update(
            @PathVariable Long noticeId,
            @Valid @RequestBody NoticeUpdateRequest request) {
        NoticeVO vo = noticeService.updateNotice(noticeId, request);
        return ApiResponse.success("更新成功", vo);
    }

    @DeleteMapping("/{noticeId}")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public ApiResponse<Void> delete(@PathVariable Long noticeId) {
        noticeService.deleteNotice(noticeId);
        return ApiResponse.success("删除成功", null);
    }

    private Long getCurrentUserId() {
        return (Long) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();
    }
}
