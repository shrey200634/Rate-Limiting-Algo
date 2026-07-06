local key = KEYS[1]
local window = tonumber(ARGV[1])
local limit = tonumber(ARGV[2])
local now = tonumber(ARGV[3])
-- Determine current and previous window boundaries
local current_window = math.floor(now / window) * window
local prev_window = current_window - window
-- Build Redis keys for each window's counter
local curr_key = key .. ':' .. current_window
local prev_key = key .. ':' .. prev_window
-- Get counts
local prev_count = tonumber(redis.call('GET', prev_key) or '0') or 0
local curr_count = tonumber(redis.call('GET', curr_key) or '0') or 0
-- How far into the current window are we? (0.0 to 1.0)
local elapsed_ratio = (now - current_window) / window
-- Weighted count: previous window's contribution decays linearly
local weight = 1 - elapsed_ratio
local weighted_count = prev_count * weight + curr_count
if weighted_count < limit then
    -- Allow: increment the current window counter
    redis.call('INCR', curr_key)
    -- Set expiry to 2 windows (so previous window data is available for the next window)
    redis.call('EXPIRE', curr_key, window * 2)
    local remaining = math.floor(limit - weighted_count - 1)
    if remaining < 0 then remaining = 0 end
    -- Time until current window ends
    local reset_seconds = current_window + window - now
    return {1, remaining, reset_seconds}
else
    -- Denied
    local reset_seconds = current_window + window - now
    return {0, 0, reset_seconds}
end