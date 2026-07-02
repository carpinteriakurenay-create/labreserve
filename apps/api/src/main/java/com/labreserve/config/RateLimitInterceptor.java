package com.labreserve.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Simple in-memory rate limit interceptor keyed by client IP.
 * Production should replace this with Redis-based counting.
 * Test requests from 127.0.0.1 bypass rate limiting.
 */
@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    private static final int LOGIN_LIMIT = 10;
    private static final int REGISTER_LIMIT = 3;
    private static final int GLOBAL_LIMIT = 120;
    private static final long WINDOW_MS = 60_000L;

    private final Map<String, WindowCounter> counters = new ConcurrentHashMap<>();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
                             Object handler) throws Exception {

        String clientIp = getClientIp(request);

        // Bypass rate limiting for local/test requests
        if ("127.0.0.1".equals(clientIp) || "0:0:0:0:0:0:0:1".equals(clientIp)) {
            return true;
        }
        String path = request.getRequestURI();
        long now = System.currentTimeMillis();

        int limit;
        String key;

        if (path.equals("/api/auth/login")) {
            limit = LOGIN_LIMIT;
            key = "login:" + clientIp;
        } else if (path.equals("/api/auth/register")) {
            limit = REGISTER_LIMIT;
            key = "register:" + clientIp;
        } else {
            limit = GLOBAL_LIMIT;
            key = "global:" + clientIp;
        }

        WindowCounter counter = counters.computeIfAbsent(key, k -> new WindowCounter(now));

        synchronized (counter) {
            if (now - counter.windowStart > WINDOW_MS) {
                counter.windowStart = now;
                counter.count = 0;
            }

            counter.count++;

            if (counter.count > limit) {
                response.setContentType("application/json;charset=UTF-8");
                response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                response.getWriter().write(
                        "{\"code\":\"RATE_LIMITED\",\"message\":\"请求过于频繁，请稍后再试\",\"details\":null}"
                );
                return false;
            }
        }

        // Cleanup stale entries periodically (every ~100 requests)
        if (Math.random() < 0.01) {
            counters.entrySet().removeIf(e ->
                    now - e.getValue().windowStart > WINDOW_MS * 2);
        }

        return true;
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isBlank()) {
            return xRealIp.trim();
        }
        return request.getRemoteAddr();
    }

    private static class WindowCounter {
        long windowStart;
        int count;

        WindowCounter(long windowStart) {
            this.windowStart = windowStart;
            this.count = 0;
        }
    }
}
