package com.ratelimiter.sliding_window_log.config;

import com.ratelimiter.common.lua.LuaScriptLoader;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.script.RedisScript;

import java.util.List;

@Configuration
public class RedisScriptConfig {

    @Bean
    public RedisScript<List> slidingWindowLog(){
        return LuaScriptLoader.load("lua/sliding-window-log.lua" , List.class);
    }
}
