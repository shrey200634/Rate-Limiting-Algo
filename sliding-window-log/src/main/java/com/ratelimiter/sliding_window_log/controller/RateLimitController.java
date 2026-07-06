package com.ratelimiter.sliding_window_log.controller;


import com.ratelimiter.common.dto.RateLimitResult;
import com.ratelimiter.sliding_window_log.service.SlidingWindowLogService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RateLimitController {

    private  final SlidingWindowLogService service ;

    public  RateLimitController(SlidingWindowLogService service){
        this.service=service ;
    }

    @GetMapping("/api/check")
    public ResponseEntity<RateLimitResult> check (@RequestParam String userId ){
        RateLimitResult result = service.tryAcquire(userId );

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
