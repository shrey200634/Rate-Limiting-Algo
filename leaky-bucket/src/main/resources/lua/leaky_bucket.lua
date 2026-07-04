-- KEYS[1] = bucket key, e.g. "ratelimit:leaky:user123"
-- ARGV[1] = capacity (max volume the bucket can hold)
-- ARGV[2] = leak_rate (units drained per second)
-- ARGV[3] = now (current epoch time in seconds, passed from Java)

local key = KEYS[1]
local capacity = tonumber(ARGV[1])
local leak_rate = tonumber(ARGV[2])
local now = tonumber(ARGV[3])

local bucket = redis.call('HMGET', key, 'volume', 'last_leak')
local volume = tonumber(bucket[1])
local last_leak = tonumber(bucket[2])

-- First time this key is seen: bucket starts empty
if volume == nil then
    volume = 0
    last_leak = now
end

-- How much has leaked out since we last touched this bucket?
local elapsed = math.max(0, now - last_leak)
local leaked = elapsed * leak_rate

-- Drain the bucket by the leaked amount, floor at 0
volume = math.max(0, volume - leaked)

local allowed = 0
if volume + 1 <= capacity then
    volume = volume + 1
    allowed = 1
end

-- Persist new state
redis.call('HSET', key, 'volume', volume, 'last_leak', now)

local ttl = math.ceil(capacity / leak_rate)
redis.call('EXPIRE', key, ttl)

return {allowed, math.floor(capacity - volume), ttl}