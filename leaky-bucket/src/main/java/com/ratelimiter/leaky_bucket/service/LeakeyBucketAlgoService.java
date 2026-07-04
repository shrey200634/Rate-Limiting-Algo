package com.ratelimiter.leaky_bucket.service;

import com.ratelimiter.common.dto.RateLimitResult;
import com.ratelimiter.common.util.TimeProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
public class LeakeyBucketAlgoService {

    private final RedisScript<List> redisScript;
    private final RedisTemplate redisTemplate ;
    private final TimeProvider timeProvider ;

    @Value("${ratelimit.leaky-bucket.capacity:5}")
    private int capacity;

    @Value("${ratelimit.leaky-bucket.leak-rate:1}")
    private double leakRate;

    public  LeakeyBucketAlgoService(RedisScript<List> redisScript , RedisTemplate redisTemplate , TimeProvider timeProvider ){
        this.redisScript= redisScript ;
        this.redisTemplate = redisTemplate ;
        this.timeProvider = timeProvider ;

    }

    public RateLimitResult tryAcquire ( String id ){
        String key = "ratelimit:leaky:" + id ;
        long now = timeProvider.nowEpochSecond() ;

        @SuppressWarnings("unchecked")
                List<Long> res = (List<Long>) redisTemplate.execute(
                        redisScript,
                Collections.singletonList(key),
                String.valueOf(capacity),
                String.valueOf(leakRate),
                String.valueOf(now)
        );
        long allowedFlag  = res.get(0);
        long roomRemaining = res.get(1);
        long ttl = res.get(2);

        boolean allowd = allowedFlag ==1 ;
        long reseAt = now+ ttl;

        return  new RateLimitResult(allowd, roomRemaining , reseAt);
    }


}
