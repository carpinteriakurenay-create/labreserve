package com.labreserve.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private final JwtUtil jwtUtil;
    private final RedissonClient redissonClient;

    public JwtAuthenticationFilter(JwtUtil jwtUtil,
                                    @org.springframework.beans.factory.annotation.Autowired(required = false)
                                    RedissonClient redissonClient) {
        this.jwtUtil = jwtUtil;
        this.redissonClient = redissonClient;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);

        if (jwtUtil.isTokenValid(token)) {
            Long userId = jwtUtil.extractUserId(token);

            // Check token version: if user has changed password since this token
            // was issued, reject the token
            long tokenVersion = jwtUtil.extractTokenVersion(token);
            if (!isTokenVersionValid(userId, tokenVersion)) {
                filterChain.doFilter(request, response);
                return;
            }

            String username = jwtUtil.extractUsername(token);
            String role = jwtUtil.extractRole(token);

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            userId,
                            null,
                            List.of(new SimpleGrantedAuthority("ROLE_" + role))
                    );

            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        filterChain.doFilter(request, response);
    }

    private boolean isTokenVersionValid(Long userId, long tokenVersion) {
        if (tokenVersion == 0L || redissonClient == null) {
            return true;
        }
        try {
            RBucket<Long> bucket = redissonClient.getBucket("user:" + userId + ":tokenVersion");
            Long storedVersion = bucket.get();
            if (storedVersion == null) return true; // Redis unavailable — allow through
            return tokenVersion >= storedVersion;
        } catch (Exception e) {
            // Redis unavailable — allow through gracefully
            log.warn("Failed to check token version for user {}: {}", userId, e.getMessage());
            return true;
        }
    }
}
