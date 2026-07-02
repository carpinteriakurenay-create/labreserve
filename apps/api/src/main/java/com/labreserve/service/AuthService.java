package com.labreserve.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.labreserve.config.JwtUtil;
import com.labreserve.dto.ChangePasswordRequest;
import com.labreserve.dto.LoginRequest;
import com.labreserve.dto.LoginResponse;
import com.labreserve.dto.RegisterRequest;
import com.labreserve.dto.UserInfo;
import com.labreserve.entity.User;
import com.labreserve.enums.UserRole;
import com.labreserve.exception.BusinessException;
import com.labreserve.mapper.UserMapper;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;
    private final RedissonClient redissonClient;

    public AuthService(UserMapper userMapper, PasswordEncoder passwordEncoder,
                       JwtUtil jwtUtil, AuthenticationManager authenticationManager,
                       @Autowired(required = false) RedissonClient redissonClient) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.authenticationManager = authenticationManager;
        this.redissonClient = redissonClient;
    }

    public void register(RegisterRequest request) {
        Long count = userMapper.selectCount(
                new LambdaQueryWrapper<User>().eq(User::getUsername, request.getUsername())
        );
        if (count > 0) {
            throw new BusinessException("USERNAME_EXISTS", "用户名已被注册");
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRealName(request.getRealName());
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        user.setRole(UserRole.STUDENT);
        user.setEnabled(1);

        userMapper.insert(user);
    }

    public LoginResponse login(LoginRequest request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
            );
        } catch (BadCredentialsException e) {
            throw new BusinessException("INVALID_CREDENTIALS", "用户名或密码错误");
        } catch (DisabledException e) {
            throw new BusinessException("ACCOUNT_DISABLED", "账号已被禁用");
        }

        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getUsername, request.getUsername())
        );

        String token = jwtUtil.generateToken(user.getId(), user.getUsername(),
                user.getRole().name(), getTokenVersion(user.getId()));

        return LoginResponse.builder()
                .token(token)
                .expiresIn(jwtUtil.getExpiration() / 1000)
                .user(toUserInfo(user))
                .build();
    }

    public UserInfo getCurrentUser(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("NOT_FOUND", "用户不存在");
        }
        return toUserInfo(user);
    }

    public void changePassword(Long userId, ChangePasswordRequest request) {
        User user = userMapper.selectById(userId);
        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw new BusinessException("WRONG_PASSWORD", "原密码错误");
        }
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userMapper.updateById(user);

        // Invalidate all existing tokens by incrementing the token version in Redis
        incrementTokenVersion(userId);
    }

    private long getTokenVersion(Long userId) {
        try {
            RBucket<Long> bucket = redissonClient.getBucket("user:" + userId + ":tokenVersion");
            Long version = bucket.get();
            return version != null ? version : 0L;
        } catch (Exception e) {
            log.warn("Failed to get token version for user {}: {}", userId, e.getMessage());
            return 0L;
        }
    }

    private void incrementTokenVersion(Long userId) {
        try {
            RBucket<Long> bucket = redissonClient.getBucket("user:" + userId + ":tokenVersion");
            long current = bucket.get() != null ? bucket.get() : 0L;
            // Store new version with TTL matching JWT expiration + buffer
            Duration ttl = Duration.ofMillis(jwtUtil.getExpiration()).plusMinutes(5);
            bucket.set(current + 1, ttl);
        } catch (Exception e) {
            log.warn("Failed to increment token version for user {}: {}", userId, e.getMessage());
        }
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
