package com.labreserve.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.labreserve.dto.ApiResponse;
import com.labreserve.dto.CourseCreateRequest;
import com.labreserve.dto.CourseUpdateRequest;
import com.labreserve.dto.CourseVO;
import com.labreserve.service.CourseService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.GrantedAuthority;
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
@RequestMapping("/api/courses")
public class CourseController {

    private final CourseService courseService;

    public CourseController(CourseService courseService) {
        this.courseService = courseService;
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<IPage<CourseVO>> list(
            @RequestParam(required = false) String semester,
            @RequestParam(required = false) Long teacherId,
            @RequestParam(required = false) Long labId,
            @RequestParam(required = false) String className,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize) {
        IPage<CourseVO> page = courseService.listCourses(
                semester, teacherId, labId, className, pageNum, pageSize);
        return ApiResponse.success(page);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public ResponseEntity<ApiResponse<CourseVO>> create(
            @Valid @RequestBody CourseCreateRequest request) {
        CourseVO vo = courseService.createCourse(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("创建成功", vo));
    }

    @GetMapping("/mine")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<IPage<CourseVO>> listMine(
            @RequestParam(required = false) String className,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize) {
        Long userId = getCurrentUserId();
        String role = getCurrentUserRole();
        IPage<CourseVO> page = courseService.listMyCourses(userId, role, className, pageNum, pageSize);
        return ApiResponse.success(page);
    }

    @GetMapping("/{courseId}")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<CourseVO> getById(@PathVariable Long courseId) {
        CourseVO vo = courseService.getCourseById(courseId);
        return ApiResponse.success(vo);
    }

    @PutMapping("/{courseId}")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public ApiResponse<CourseVO> update(
            @PathVariable Long courseId,
            @Valid @RequestBody CourseUpdateRequest request) {
        CourseVO vo = courseService.updateCourse(courseId, request);
        return ApiResponse.success("更新成功", vo);
    }

    @DeleteMapping("/{courseId}")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public ApiResponse<Void> delete(@PathVariable Long courseId) {
        courseService.deleteCourse(courseId);
        return ApiResponse.success("删除成功", null);
    }

    private Long getCurrentUserId() {
        return (Long) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();
    }

    private String getCurrentUserRole() {
        return SecurityContextHolder.getContext()
                .getAuthentication().getAuthorities().stream()
                .findFirst()
                .map(GrantedAuthority::getAuthority)
                .map(a -> a.replace("ROLE_", ""))
                .orElse("STUDENT");
    }
}
