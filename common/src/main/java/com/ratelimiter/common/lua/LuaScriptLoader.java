package com.ratelimiter.common.lua;

import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;

public class LuaScriptLoader {

    public  static <T>RedisScript<T> load (String classPathLocation , Class<T> resultType){
        DefaultRedisScript<T> script = new DefaultRedisScript<>();
        script.setLocation( new ClassPathResource(classPathLocation));
        script.setResultType(resultType);
        return script;
    }
}
