local key = KEYS[1]
local limit = tonumber(ARGV[1])
local window = tonumber(ARGV[2])

local current = redis.call('INCR', key)

if current == 1 then
    redis.call('EXPIRE', key, window)
end

local ttl = redis.call('TTL', key)

if current > limit then
    return {0, current, ttl}   -- denied
else
    return {1, current, ttl}   -- allowed
end