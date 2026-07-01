package com.labreserve.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.labreserve.dto.UserCreateRequest;
import com.labreserve.dto.UserInfo;
import com.labreserve.dto.UserUpdateRequest;
import com.labreserve.entity.User;
import com.labreserve.enums.UserRole;
import com.labreserve.exception.BusinessException;
import com.labreserve.mapper.UserMapper;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserMapper userMapper, PasswordEncoder passwordEncoder) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
    }

    public IPage<UserInfo> listUsers(UserRole role, Boolean enabled, int pageNum, int pageSize) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        if (role != null) {
            wrapper.eq(User::getRole, role);
        }
        if (enabled != null) {
            wrapper.eq(User::getEnabled, enabled ? 1 : 0);
        }
        wrapper.orderByDesc(User::getCreatedAt);

        Page<User> page = new Page<>(pageNum, pageSize);
        IPage<User> result = userMapper.selectPage(page, wrapper);
        return result.convert(this::toUserInfo);
    }

    @Cacheable(value = "user", key = "#id")
    public UserInfo getUserById(Long id) {
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException("NOT_FOUND", "用户不存在");
        }
        return toUserInfo(user);
    }

    @Transactional
    @CacheEvict(value = "user", allEntries = true)
    public UserInfo createUser(UserCreateRequest request) {
        Long count = userMapper.selectCount(
                new LambdaQueryWrapper<User>().eq(User::getUsername, request.getUsername())
        );
        if (count > 0) {
            throw new BusinessException("USERNAME_EXISTS", "用户名已被使用");
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRealName(request.getRealName());
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        user.setRole(request.getRole() != null ? request.getRole() : UserRole.STUDENT);
        user.setEnabled(1);

        userMapper.insert(user);
        return toUserInfo(user);
    }

    @Transactional
    @CacheEvict(value = "user", key = "#id")
    public UserInfo updateUser(Long id, UserUpdateRequest request) {
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException("NOT_FOUND", "用户不存在");
        }

        LambdaUpdateWrapper<User> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(User::getId, id);

        if (request.getRealName() != null) {
            wrapper.set(User::getRealName, request.getRealName());
        }
        if (request.getEmail() != null) {
            wrapper.set(User::getEmail, request.getEmail());
        }
        if (request.getPhone() != null) {
            wrapper.set(User::getPhone, request.getPhone());
        }
        if (request.getRole() != null) {
            wrapper.set(User::getRole, request.getRole());
        }
        if (request.getEnabled() != null) {
            wrapper.set(User::getEnabled, request.getEnabled() ? 1 : 0);
        }

        userMapper.update(wrapper);

        User updated = userMapper.selectById(id);
        return toUserInfo(updated);
    }

    @Transactional
    @CacheEvict(value = "user", key = "#id")
    public void deleteUser(Long id) {
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException("NOT_FOUND", "用户不存在");
        }
        userMapper.deleteById(id);
    }

    @Transactional
    @CacheEvict(value = "user", key = "#id")
    public void toggleEnabled(Long id) {
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException("NOT_FOUND", "用户不存在");
        }
        int newEnabled = user.getEnabled() == 1 ? 0 : 1;
        LambdaUpdateWrapper<User> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(User::getId, id).set(User::getEnabled, newEnabled);
        userMapper.update(wrapper);
    }

    private UserInfo toUserInfo(User user) {
        return UserInfo.builder()
                .id(user.getId())
                .username(user.getUsername())
                .realName(user.getRealName())
                .role(user.getRole().name())
                .avatar(user.getAvatar())
                .email(user.getEmail())
                .phone(user.getPhone())
                .enabled(user.getEnabled() == 1)
                .build();
    }
}
