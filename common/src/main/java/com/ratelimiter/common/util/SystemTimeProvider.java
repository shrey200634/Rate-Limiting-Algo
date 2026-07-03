package com.ratelimiter.common.util;

import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class SystemTimeProvider  implements  TimeProvider{
    @Override
    public  long nowEpochSecond(){
        return Instant.now().getEpochSecond();
    }
}
