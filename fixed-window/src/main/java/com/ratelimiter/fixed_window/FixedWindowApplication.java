package com.ratelimiter.fixed_window;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {"com.ratelimiter.fixed_window", "com.ratelimiter.common"})

public class FixedWindowApplication {

	public static void main(String[] args) {
		SpringApplication.run(FixedWindowApplication.class, args);
	}

}
