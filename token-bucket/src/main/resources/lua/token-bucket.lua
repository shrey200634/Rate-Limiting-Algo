-- KEYS[1] = bucket key, e.g. "ratelimit:token:user123"
-- ARGV[1] = capacity (max tokens the bucket can hold)
-- ARGV[2] = refill_rate (tokens added per second)
-- ARGV[3] = now (current epoch time in seconds, passed from Java — NOT redis.call('TIME'))
-- ARGV[4] = requested tokens (usually 1)

local key = KEYS[1]
local capacity = tonumber(ARGV[1])
local refill_rate = tonumber(ARGV[2])
local now = tonumber(ARGV[3])
local requested = tonumber(ARGV[4])

local bucket = redis.call('HMGET', key, 'tokens', 'last_refill')
local tokens = tonumber(bucket[1])
local last_refill = tonumber(bucket[2])

-- First time this key is seen: bucket starts full
if tokens == nil then
    tokens = capacity
    last_refill = now
end

-- How much time has passed since we last touched this bucket?
local elapsed = math.max(0, now - last_refill)

-- Refill proportionally to elapsed time, capped at capacity
tokens = math.min(capacity, tokens + (elapsed * refill_rate))

local allowed = 0
if tokens >= requested then
    tokens = tokens - requested
    allowed = 1
end

-- Persist new state
redis.call('HSET', key, 'tokens', tokens, 'last_refill', now)

-- Bucket should expire if unused long enough that it would be full anyway
-- (avoids leaving stale keys around forever)
local ttl = math.ceil(capacity / refill_rate)
redis.call('EXPIRE', key, ttl)

return {allowed, math.floor(tokens), ttl}