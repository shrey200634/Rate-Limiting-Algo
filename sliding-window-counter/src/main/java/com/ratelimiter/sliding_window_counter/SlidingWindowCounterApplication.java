package com.ratelimiter.sliding_window_counter;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {"com.ratelimiter.sliding_window_counter", "com.ratelimiter.common"})

public class SlidingWindowCounterApplication {

	public static void main(String[] args) {
		SpringApplication.run(SlidingWindowCounterApplication.class, args);
	}

}
