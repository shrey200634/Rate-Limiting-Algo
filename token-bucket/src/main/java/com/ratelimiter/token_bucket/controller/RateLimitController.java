package com.ratelimiter.token_bucket.controller;


import com.ratelimiter.common.dto.RateLimitResult;
import com.ratelimiter.token_bucket.service.TokenBucketRateLimiterService;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RateLimitController {

    private final TokenBucketRateLimiterService tokenBucketRateLimiterService ;

    public  RateLimitController(TokenBucketRateLimiterService tokenBucketRateLimiterService){
        this.tokenBucketRateLimiterService=tokenBucketRateLimiterService;
    }


    @GetMapping("/api/check")
    public ResponseEntity<RateLimitResult> check (@RequestParam String userId ){
        RateLimitResult result = tokenBucketRateLimiterService.tryAcquire(userId);

        HttpHeaders headers = new HttpHeaders();
        headers.add("X-RateLimit-Remaining", String.valueOf(result.remaining()));
        headers.add("X-RateLimit-Reset", String.valueOf(result.resetAtEpochSeconds()));

        if (!result.allowed()){
            headers.add("Retry after", String.valueOf(result.resetAtEpochSeconds()));
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .headers(headers)
                    .body(result);
        }
        return ResponseEntity.ok().headers(headers).body(result);
    }


}
