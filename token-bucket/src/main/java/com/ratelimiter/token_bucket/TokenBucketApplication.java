package com.ratelimiter.token_bucket;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {"com.ratelimiter.token_bucket", "com.ratelimiter.common"})

public class TokenBucketApplication {

	public static void main(String[] args) {
		SpringApplication.run(TokenBucketApplication.class, args);
	}

}
