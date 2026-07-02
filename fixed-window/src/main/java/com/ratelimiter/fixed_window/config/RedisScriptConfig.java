package com.ratelimiter.fixed_window.config;
import com.ratelimiter.common.lua.LuaScriptLoader;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.script.RedisScript;

import java.util.List;

@Configuration
public class RedisScriptConfig {

    @Bean
    public RedisScript<List> fixedWindowScript(){
        return LuaScriptLoader.load("lua/fixed_window.lua" , List.class);

    }
}
