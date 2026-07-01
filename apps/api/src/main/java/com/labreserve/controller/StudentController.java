package com.labreserve.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.labreserve.dto.ApiResponse;
import com.labreserve.dto.StudentCreateRequest;
import com.labreserve.dto.StudentUpdateRequest;
import com.labreserve.dto.StudentVO;
import com.labreserve.service.StudentService;
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
@RequestMapping("/api/students")
public class StudentController {

    private final StudentService studentService;

    public StudentController(StudentService studentService) {
        this.studentService = studentService;
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<IPage<StudentVO>> list(
            @RequestParam(required = false) Long labId,
            @RequestParam(required = false) String name,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize) {
        IPage<StudentVO> page = studentService.listStudents(labId, name, pageNum, pageSize);
        return ApiResponse.success(page);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public ResponseEntity<ApiResponse<StudentVO>> create(
            @Valid @RequestBody StudentCreateRequest request) {
        Long creatorId = getCurrentUserId();
        StudentVO vo = studentService.createStudent(request, creatorId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("录入成功", vo));
    }

    @GetMapping("/{studentId}")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<StudentVO> getById(@PathVariable Long studentId) {
        StudentVO vo = studentService.getStudentById(studentId);
        return ApiResponse.success(vo);
    }

    @PutMapping("/{studentId}")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public ApiResponse<StudentVO> update(
            @PathVariable Long studentId,
            @Valid @RequestBody StudentUpdateRequest request) {
        StudentVO vo = studentService.updateStudent(studentId, request);
        return ApiResponse.success("更新成功", vo);
    }

    @DeleteMapping("/{studentId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> delete(@PathVariable Long studentId) {
        studentService.deleteStudent(studentId);
        return ApiResponse.success("删除成功", null);
    }

    private Long getCurrentUserId() {
        return (Long) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();
    }
}
