package com.ratelimiter.sliding_window_log;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {"com.ratelimiter.sliding_window_log", "com.ratelimiter.common"})

public class SlidingWindowLogApplication {

	public static void main(String[] args) {
		SpringApplication.run(SlidingWindowLogApplication.class, args);
	}

}
