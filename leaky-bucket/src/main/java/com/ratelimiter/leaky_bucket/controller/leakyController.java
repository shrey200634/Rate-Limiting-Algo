package com.ratelimiter.leaky_bucket.controller;

import com.ratelimiter.common.dto.RateLimitResult;
import com.ratelimiter.leaky_bucket.service.LeakeyBucketAlgoService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class leakyController {

    private final LeakeyBucketAlgoService leakeyBucketAlgoService;

    public  leakyController(LeakeyBucketAlgoService leakeyBucketAlgoService){
        this.leakeyBucketAlgoService = leakeyBucketAlgoService;
    }




    @GetMapping("/api/check")
    public ResponseEntity<RateLimitResult> check(@RequestParam String userId) {
        RateLimitResult result = leakeyBucketAlgoService.tryAcquire(userId);

        HttpHeaders headers = new HttpHeaders();
        headers.add("X-RateLimit-Remaining", String.valueOf(result.remaining()));
        headers.add("X-RateLimit-Reset", String.valueOf(result.resetAtEpochSeconds()));

        if (!result.allowed()) {
            headers.add("Retry-After", String.valueOf(result.resetAtEpochSeconds()));
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .headers(headers)
                    .body(result);
        }

        return ResponseEntity.ok().headers(headers).body(result);
    }
}