package com.ratelimiter.sliding_window_counter.controller;


import com.ratelimiter.common.dto.RateLimitResult;
import com.ratelimiter.sliding_window_counter.service.SlidingWindowCounterService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RateController {

    private final SlidingWindowCounterService service ;

    public  RateController(SlidingWindowCounterService service){
        this.service=service;
    }

    @GetMapping("/api/check")
    public ResponseEntity<RateLimitResult> check(String userId ){
        RateLimitResult result= service.tryAquire(userId);
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
