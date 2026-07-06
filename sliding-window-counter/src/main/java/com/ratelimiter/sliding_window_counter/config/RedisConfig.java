package com.ratelimiter.sliding_window_counter.config;


import com.ratelimiter.common.lua.LuaScriptLoader;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.script.RedisScript;

import java.util.List;

@Configuration
public class RedisConfig {

    @Bean
    public RedisScript<List>  script (){
        return LuaScriptLoader.load("lua/rateLimit.lua" , List.class);
    }
}
