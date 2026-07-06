package com.ratelimiter.sliding_window_log.service;

import com.ratelimiter.common.dto.RateLimitResult;
import com.ratelimiter.common.util.TimeProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
public class slidingWindowLogService {

    private final RedisScript<List> redisScript ;
    private final StringRedisTemplate stringRedisTemplate ;
    private final TimeProvider timeProvider ;

    @Value("${ratelimit.sliding-window-log.limit:5}")
    private int limit;
    @Value("${ratelimit.sliding-window-log.window-seconds:60}")
    private int windowSeconds;

    public  slidingWindowLogService(RedisScript<List> redisScript , StringRedisTemplate stringRedisTemplate ,
                                    TimeProvider timeProvider){
        this.redisScript = redisScript ;
        this.stringRedisTemplate = stringRedisTemplate ;
        this.timeProvider = timeProvider ;

    }

    public RateLimitResult tryAcquire(String identifier ){
        String key = "ratelimit:swlog:" + identifier;
        long now = timeProvider.nowEpochSecond();

        @SuppressWarnings("unchecked")
                List<Long> res = stringRedisTemplate.execute(
                        redisScript,
                Collections.singletonList(key),
                String.valueOf(windowSeconds),
                String.valueOf(limit),
                String.valueOf(now)
        );
        long allowedFlag = res.get(0);
        long remaining = res.get(1);
        long ttl = res.get(2);
        boolean allowed = allowedFlag == 1;
        long resetAt = now + ttl;
        return new RateLimitResult(allowed, remaining, resetAt);
    }


}
