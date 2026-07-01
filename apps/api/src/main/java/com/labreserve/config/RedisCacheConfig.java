package com.labreserve.config;

import org.redisson.api.RedissonClient;
import org.redisson.spring.cache.CacheConfig;
import org.redisson.spring.cache.RedissonSpringCacheManager;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class RedisCacheConfig {

    private static final long LAB_CACHE_TTL = 5 * 60 * 1000;
    private static final long USER_CACHE_TTL = 10 * 60 * 1000;
    private static final long KPI_CACHE_TTL = 5 * 60 * 1000;

    @Bean
    public CacheManager cacheManager(RedissonClient redissonClient) {
        Map<String, CacheConfig> config = new HashMap<>();
        config.put("lab", new CacheConfig(LAB_CACHE_TTL, 0));
        config.put("labHours", new CacheConfig(LAB_CACHE_TTL, 0));
        config.put("user", new CacheConfig(USER_CACHE_TTL, 0));
        config.put("kpi", new CacheConfig(KPI_CACHE_TTL, 0));
        return new RedissonSpringCacheManager(redissonClient, config);
    }
}
