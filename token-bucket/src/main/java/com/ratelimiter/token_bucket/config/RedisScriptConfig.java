package com.ratelimiter.token_bucket.config;


import com.ratelimiter.common.lua.LuaScriptLoader;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.script.RedisScript;

import java.util.List;

@Configuration
public class RedisScriptConfig {

    @Bean
    public RedisScript<List> tokenBucketScript(){
        return LuaScriptLoader.load("lua/token-bucket.lua" , List.class);
    }
}
