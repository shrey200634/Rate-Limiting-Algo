package com.ratelimiter.fixed_window.service;

import com.ratelimiter.common.dto.RateLimitResult;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
@Service
public class FixedWindowRateLimiterService {

    private final StringRedisTemplate redisTemplate ;
    private final RedisScript<List> fixedWindowScript ;

    @Value("${ratelimit.fixed-window.limit:5}")
    private int limit ;

    @Value("${ratelimit.fixed-window.window-seconds:60}")
    private int windowSeconds ;

     public FixedWindowRateLimiterService(StringRedisTemplate redisTemplate , RedisScript<List> fixedWindowScript){
         this.fixedWindowScript=fixedWindowScript;
           this.redisTemplate= redisTemplate;
          }
    public RateLimitResult tryAcquire(String identifier ){
        String key = "ratelimit:fixed:" + identifier;

        @SuppressWarnings("unchecked")
                List<Long> res = redisTemplate.execute(
                        fixedWindowScript,
                Collections.singletonList(key),
                String.valueOf(limit),
                String.valueOf(windowSeconds)
        );

        long allowedFlag = res.get(0);
        long currentCount = res.get(1);
        long ttl = res.get(2);

        boolean allowd = allowedFlag==1 ;
        long remaining = Math.max(0,limit-currentCount);
        long resetAt = Instant.now().getEpochSecond() + ttl;

        return new RateLimitResult(allowd , remaining, resetAt);
    }


}
