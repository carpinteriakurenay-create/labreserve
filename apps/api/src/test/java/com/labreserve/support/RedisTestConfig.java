package com.labreserve.support;

import org.mockito.Mockito;
import org.redisson.api.RBucket;
import org.redisson.api.RLock;
import org.redisson.api.RMapCache;
import org.redisson.api.RedissonClient;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cache.CacheManager;
import org.springframework.cache.support.NoOpCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.concurrent.TimeUnit;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;

@TestConfiguration
public class RedisTestConfig {

    @Bean
    @Primary
    public CacheManager noOpCacheManager() {
        return new NoOpCacheManager();
    }

    @Bean
    @Primary
    public RedissonClient testRedissonClient() {
        RedissonClient mockClient = mock(RedissonClient.class);

        // Lock
        RLock mockLock = mock(RLock.class);
        try {
            lenient().when(mockLock.tryLock(anyLong(), any(TimeUnit.class))).thenReturn(true);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        lenient().when(mockClient.getLock(anyString())).thenReturn(mockLock);

        // Bucket (for token version, etc.)
        RBucket<Object> mockBucket = mock(RBucket.class);
        lenient().when(mockClient.getBucket(anyString())).thenReturn(mockBucket);

        // MapCache — return null (RedissonSpringCacheManager is not loaded in test)
        lenient().when(mockClient.getMapCache(anyString())).thenReturn(null);

        return mockClient;
    }

    @Bean
    @Primary
    public PasswordEncoder testPasswordEncoder() {
        return new BCryptPasswordEncoder() {
            @Override
            public boolean matches(CharSequence rawPassword, String encodedPassword) {
                return "password123".equals(rawPassword.toString());
            }

            @Override
            public String encode(CharSequence rawPassword) {
                return "$2a$10$test-password-hash-placeholder";
            }
        };
    }
}
