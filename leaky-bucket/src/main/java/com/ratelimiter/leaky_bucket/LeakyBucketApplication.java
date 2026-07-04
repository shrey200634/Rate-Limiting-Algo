package com.ratelimiter.leaky_bucket;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {"com.ratelimiter.leaky_bucket", "com.ratelimiter.common"})

public class LeakyBucketApplication {

	public static void main(String[] args) {
		SpringApplication.run(LeakyBucketApplication.class, args);
	}

}
