package com.ratelimiter.common.dto;

public record  RateLimitResult (
        boolean allowed ,
        long remaining ,
        long resetAtEpochSeconds
) {

}
