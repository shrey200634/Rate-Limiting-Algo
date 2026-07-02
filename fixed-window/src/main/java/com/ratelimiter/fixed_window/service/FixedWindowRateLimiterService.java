package com.ratelimiter.fixed_window.service;

import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;

import java.util.List;
@AllArgsConstructor
@Service
public class FixedWindowRateLimiterService {

    private final StringRedisTemplate redisTemplate ;
    private final RedisScript<List> fixedWindowScript ;

    @Value("${ratelimit.fixed-window.limit:5}")
    private int limit ;

    @Value("${ratelimit.fixed-window.window-seconds:60}")
    private int windowSeconds ;


}
