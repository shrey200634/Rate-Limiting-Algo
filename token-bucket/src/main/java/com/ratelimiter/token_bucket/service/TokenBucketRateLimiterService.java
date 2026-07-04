package com.ratelimiter.token_bucket.service;

import com.ratelimiter.common.dto.RateLimitResult;
import com.ratelimiter.common.util.TimeProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;
import java.util.Collections;
import java.util.List;

@Service
public class TokenBucketRateLimiterService {

    private final StringRedisTemplate redisTemplate;
    private final RedisScript<List> tokenBucketScript;
    private final TimeProvider timeProvider;

    @Value("${ratelimit.token-bucket.capacity:5}")
    private int capacity;

    @Value("${ratelimit.token-bucket.refill-rate:1}")
    private double refillRate;

    public TokenBucketRateLimiterService(StringRedisTemplate redisTemplate,
                                         RedisScript<List> tokenBucketScript,
                                         TimeProvider timeProvider) {
        this.redisTemplate = redisTemplate;
        this.tokenBucketScript = tokenBucketScript;
        this.timeProvider = timeProvider;
    }

    public RateLimitResult tryAcquire(String identifier) {
        String key = "ratelimit:token:" + identifier;
        long now = timeProvider.nowEpochSecond();

        @SuppressWarnings("unchecked")
        List<Long> res = redisTemplate.execute(
                tokenBucketScript,
                Collections.singletonList(key),
                String.valueOf(capacity),
                String.valueOf(refillRate),
                String.valueOf(now),
                "1" // requesting 1 token
        );

        long allowedFlag = res.get(0);
        long tokensRemaining = res.get(1);
        long ttl = res.get(2);

        boolean allowed = allowedFlag == 1;
        long resetAt = now + ttl;

        return new RateLimitResult(allowed, tokensRemaining, resetAt);
    }
}