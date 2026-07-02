package com.ratelimiter.fixed_window.controller;

import com.ratelimiter.common.dto.RateLimitResult;
import com.ratelimiter.fixed_window.service.FixedWindowRateLimiterService;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@AllArgsConstructor
@RestController
public class RateLimitController {
    private final FixedWindowRateLimiterService fixedWindowRateLimiterService;


    @GetMapping("/api/check")
    public ResponseEntity<RateLimitResult> check (@RequestParam String userId ){
        RateLimitResult result = fixedWindowRateLimiterService.tryAcquire(userId);

        HttpHeaders headers= new HttpHeaders();
        headers.add("X-RateLimit-Remaining" , String.valueOf(result.remaining()));
        headers.add("X-RateLimit-Reset", String.valueOf(result.resetAtEpochSeconds()));

        if (!result.allowed()){
            headers.add("Retry-after" , String.valueOf(result.resetAtEpochSeconds()));
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .headers(headers)
                    .body(result);
        }
        return ResponseEntity.ok().headers(headers).body(result);
    }
}
