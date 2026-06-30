package com.labreserve.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.labreserve.dto.ApiResponse;
import com.labreserve.dto.UserCreateRequest;
import com.labreserve.dto.UserInfo;
import com.labreserve.dto.UserUpdateRequest;
import com.labreserve.enums.UserRole;
import com.labreserve.service.UserService;
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
@RequestMapping("/api/users")
@PreAuthorize("hasRole('ADMIN')")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public ApiResponse<IPage<UserInfo>> list(
            @RequestParam(required = false) UserRole role,
            @RequestParam(required = false) Boolean enabled,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize) {
        IPage<UserInfo> page = userService.listUsers(role, enabled, pageNum, pageSize);
        return ApiResponse.success(page);
    }

    @PostMapping
    public ResponseEntity<ApiResponse<UserInfo>> create(@Valid @RequestBody UserCreateRequest request) {
        UserInfo userInfo = userService.createUser(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("创建成功", userInfo));
    }

    @GetMapping("/{userId}")
    public ApiResponse<UserInfo> getById(@PathVariable Long userId) {
        UserInfo userInfo = userService.getUserById(userId);
        return ApiResponse.success(userInfo);
    }

    @PutMapping("/{userId}")
    public ApiResponse<UserInfo> update(
            @PathVariable Long userId,
            @Valid @RequestBody UserUpdateRequest request) {
        UserInfo userInfo = userService.updateUser(userId, request);
        return ApiResponse.success("更新成功", userInfo);
    }

    @DeleteMapping("/{userId}")
    public ApiResponse<Void> delete(@PathVariable Long userId) {
        userService.deleteUser(userId);
        return ApiResponse.success("删除成功", null);
    }

    @PutMapping("/{userId}/toggle-enabled")
    public ApiResponse<Void> toggleEnabled(@PathVariable Long userId) {
        userService.toggleEnabled(userId);
        return ApiResponse.success(null);
    }
}
