# 🚦 Rate Limiting Algorithms

A comprehensive collection of **five production-grade rate limiting algorithms** implemented as independent Spring Boot microservices, backed by **Redis** and **Lua scripting** for atomic, high-performance request throttling.

> **Built for learning, designed for production.** Each algorithm runs as a standalone service so you can study, benchmark, and compare them side by side.

---

## 📐 Architecture Overview

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                          Gradle Multi-Module Project                        │
│                                                                             │
│  ┌──────────────────────────────────────────────────────────────────────┐   │
│  │                        common (shared library)                       │   │
│  │   RedisConfig · RateLimitResult · LuaScriptLoader · TimeProvider    │   │
│  └───────────────────────────┬──────────────────────────────────────────┘   │
│              ┌───────────────┼───────────────┬───────────────┐              │
│              │               │               │               │              │
│  ┌───────────▼──┐ ┌─────────▼────┐ ┌────────▼─────┐ ┌──────▼───────────┐  │
│  │ fixed-window │ │ token-bucket │ │ leaky-bucket │ │sliding-window-log│  │
│  │   :8081      │ │   :8082      │ │   :8083      │ │     :8084        │  │
│  └──────────────┘ └──────────────┘ └──────────────┘ └──────────────────┘  │
│                                                      ┌──────────────────┐  │
│                                                      │sliding-window-   │  │
│                                                      │   counter :8085  │  │
│                                                      └────────┬─────────┘  │
└───────────────────────────────────────────────────────────────┼─────────────┘
                                                                │
                                                         ┌──────▼───────┐
                                                         │  Redis 7     │
                                                         │  :6379       │
                                                         └──────────────┘
```

Each algorithm runs as a **standalone Spring Boot application** on its own port. All share a `common` module for Redis configuration, DTOs, Lua script loading, and time utilities.

---

## 🧰 Tech Stack

| Layer         | Technology                           |
|---------------|--------------------------------------|
| Language      | Java 21                              |
| Framework     | Spring Boot 3.5.x                    |
| Build Tool    | Gradle (multi-module)                |
| Data Store    | Redis 7 (Alpine)                     |
| Atomicity     | Redis Lua Scripts                    |
| Libraries     | Lombok, Spring Data Redis, Actuator  |
| Container     | Docker Compose (Redis)               |

---

## 🏗️ Project Structure

```
rate-limiting-algorithms/
├── common/                              # Shared library module
│   └── src/main/java/.../common/
│       ├── config/RedisConfig.java           # StringRedisTemplate bean
│       ├── dto/RateLimitResult.java          # Unified response record
│       ├── exception/RateLimitExceededException.java
│       ├── lua/LuaScriptLoader.java          # Generic Lua script loader
│       └── util/
│           ├── TimeProvider.java             # Interface for testability
│           └── SystemTimeProvider.java       # Production implementation
│
├── fixed-window/                        # Fixed Window Counter  (port 8081)
│   └── src/main/
│       ├── java/.../fixed_window/
│       │   ├── config/RedisScriptConfig.java
│       │   ├── controller/RateLimitController.java
│       │   └── service/FixedWindowRateLimiterService.java
│       └── resources/
│           ├── lua/fixed_window.lua
│           └── application.yaml
│
├── token-bucket/                        # Token Bucket           (port 8082)
│   └── src/main/
│       ├── java/.../token_bucket/
│       │   ├── config/RedisScriptConfig.java
│       │   ├── controller/RateLimitController.java
│       │   └── service/TokenBucketRateLimiterService.java
│       └── resources/
│           ├── lua/token-bucket.lua
│           └── application.yaml
│
├── leaky-bucket/                        # Leaky Bucket           (port 8083)
│   └── src/main/
│       ├── java/.../leaky_bucket/
│       │   ├── config/RedisScriptConfig.java
│       │   ├── controller/leakyController.java
│       │   └── service/LeakeyBucketAlgoService.java
│       └── resources/
│           ├── lua/leaky_bucket.lua
│           └── application.yaml
│
├── sliding-window-log/                  # Sliding Window Log     (port 8084)
│   └── src/main/
│       ├── java/.../sliding_window_log/
│       │   ├── config/RedisScriptConfig.java
│       │   ├── controller/RateLimitController.java
│       │   └── service/SlidingWindowLogService.java
│       └── resources/
│           ├── lua/sliding-window-log.lua
│           └── application.yaml
│
├── sliding-window-counter/              # Sliding Window Counter (port 8085)
│   └── src/main/
│       ├── java/.../sliding_window_counter/
│       │   ├── config/RedisConfig.java
│       │   ├── controller/RateController.java
│       │   └── service/SlidingWindowCounterService.java
│       └── resources/
│           ├── lua/rateLimit.lua
│           └── application.yaml
│
├── build.gradle                         # Root build (plugins + subproject config)
├── settings.gradle                      # Module includes
├── docker-compose.yml                   # Redis container
└── README.md
```

---

## 🚀 Getting Started

### Prerequisites

- **Java 21+**
- **Docker** (for Redis)
- **Gradle** (or use the included `gradlew` wrapper)

### 1. Start Redis

```bash
docker-compose up -d
```

This spins up a Redis 7 Alpine container on port `6379` (ephemeral mode — no disk persistence).

### 2. Build the Project

```bash
./gradlew clean build
```

### 3. Run an Algorithm

Each algorithm is an independent Spring Boot application. Run whichever one you want to test:

```bash
# Fixed Window (port 8081)
./gradlew :fixed-window:bootRun

# Token Bucket (port 8082)
./gradlew :token-bucket:bootRun

# Leaky Bucket (port 8083)
./gradlew :leaky-bucket:bootRun

# Sliding Window Log (port 8084)
./gradlew :sliding-window-log:bootRun

# Sliding Window Counter (port 8085)
./gradlew :sliding-window-counter:bootRun
```

You can run **multiple modules simultaneously** since they each bind to a different port.

---

## 🔌 API Reference

All five modules expose the **same REST endpoint** with an identical contract:

### `GET /api/check`

| Parameter | Type   | Required | Description              |
|-----------|--------|----------|--------------------------|
| `userId`  | String | ✅       | Unique client identifier |

#### ✅ Allowed Response — `200 OK`

```json
{
  "allowed": true,
  "remaining": 4,
  "resetAtEpochSeconds": 1751673600
}
```

#### 🚫 Denied Response — `429 Too Many Requests`

```json
{
  "allowed": false,
  "remaining": 0,
  "resetAtEpochSeconds": 1751673600
}
```

#### Response Headers

| Header                  | Description                                  |
|-------------------------|----------------------------------------------|
| `X-RateLimit-Remaining` | Number of requests/tokens remaining          |
| `X-RateLimit-Reset`     | Epoch timestamp when the limit resets        |
| `Retry-After`           | *(Only on 429)* When to retry                |

### Quick Test with cURL

```bash
# Hit the Token Bucket endpoint
curl -s "http://localhost:8082/api/check?userId=user123" | jq

# Rapid-fire 10 requests to see rate limiting in action
for i in $(seq 1 10); do
  curl -s -o /dev/null -w "%{http_code}\n" "http://localhost:8082/api/check?userId=user123"
done
```

---

## 📖 Algorithm Deep Dive

### 1. Fixed Window Counter

**Port:** `8081` &nbsp;|&nbsp; **Redis Key Pattern:** `ratelimit:fixed:{userId}` &nbsp;|&nbsp; **Redis Data Structure:** `STRING` (counter)

```
      Window 1 (0–60s)         Window 2 (60–120s)
  ┌───────────────────────┐ ┌───────────────────────┐
  │ ■ ■ ■ ■ ■             │ │ ■ ■                   │
  │ count=5  → DENIED     │ │ count=2  → ALLOWED    │
  └───────────────────────┘ └───────────────────────┘
```

**How it works:**
1. A counter key is created in Redis using `INCR`.
2. On the first request in a window, `EXPIRE` is set to `window-seconds`.
3. Subsequent requests increment the counter. If `count > limit`, the request is denied.
4. When the key expires, the window resets automatically.

**Configuration** (`application.yaml`):
```yaml
ratelimit:
  fixed-window:
    limit: 5               # Max requests per window
    window-seconds: 60      # Window duration in seconds
```

**Lua Script** — Atomic increment + TTL check in a single round-trip:
```lua
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
```

| Pros | Cons |
|------|------|
| Simple to understand & implement | Boundary burst problem (2× burst at window edges) |
| Very low memory footprint (1 key per user) | Not smooth — bursty traffic pattern |
| O(1) per request | No partial credit between windows |

---

### 2. Token Bucket

**Port:** `8082` &nbsp;|&nbsp; **Redis Key Pattern:** `ratelimit:token:{userId}` &nbsp;|&nbsp; **Redis Data Structure:** `HASH`

```
  Bucket (capacity=5, refill=1/sec)

  t=0   [■ ■ ■ ■ ■]  → 5 tokens (full)
  t=0   [■ ■ ■ ■ □]  → Request allowed, 4 remaining
  t=0   [■ ■ ■ □ □]  → Request allowed, 3 remaining
  t=3   [■ ■ ■ ■ ■]  → 3s elapsed → 3 refilled → capped at 5
  t=3   [■ ■ ■ ■ □]  → Request allowed, 4 remaining
```

**How it works:**
1. Each user gets a virtual "bucket" stored as a Redis hash (`tokens`, `last_refill`).
2. On each request, the script calculates elapsed time and refills tokens proportionally: `tokens = min(capacity, tokens + elapsed × refill_rate)`.
3. If enough tokens are available, one is consumed and the request is allowed.
4. The key auto-expires after `capacity / refill_rate` seconds of inactivity.

**Configuration** (`application.yaml`):
```yaml
ratelimit:
  token-bucket:
    capacity: 5             # Maximum tokens the bucket holds
    refill-rate: 1          # Tokens added per second
```

**Redis Storage:**
```
HSET ratelimit:token:user123
  tokens       4.0
  last_refill  1751673600
```

**Lua Script Highlights:**
```lua
-- Refill based on elapsed time
local elapsed = math.max(0, now - last_refill)
tokens = math.min(capacity, tokens + (elapsed * refill_rate))

-- Try to consume
if tokens >= requested then
    tokens = tokens - requested
    allowed = 1
end
```

| Pros | Cons |
|------|------|
| Allows controlled bursts up to capacity | Slightly more complex than fixed window |
| Smooth, natural traffic shaping | Two fields per key in Redis |
| Industry standard (AWS, Stripe use this) | Floating point arithmetic in Lua |

---

### 3. Leaky Bucket

**Port:** `8083` &nbsp;|&nbsp; **Redis Key Pattern:** `ratelimit:leaky:{userId}` &nbsp;|&nbsp; **Redis Data Structure:** `HASH`

```
  Bucket (capacity=5, leak_rate=1/sec)

  Incoming requests fill the bucket ↓
  ┌─────────┐
  │ ■ ■ ■   │  volume=3, room=2
  │         │  
  └────╥────┘
       ║ leaks at 1 req/sec
       ▼
```

**How it works:**
1. Each user gets a virtual "bucket" stored as a Redis hash (`volume`, `last_leak`).
2. On each request, the script drains leaked volume: `volume = max(0, volume - elapsed × leak_rate)`.
3. If `volume + 1 ≤ capacity`, the request is added (volume increases) and allowed.
4. If the bucket is full, the request is denied.
5. The key auto-expires after `capacity / leak_rate` seconds of inactivity.

**Configuration** (`application.yaml`):
```yaml
ratelimit:
  leaky-bucket:
    capacity: 5             # Maximum volume the bucket holds
    leak-rate: 1            # Units drained per second
```

**Redis Storage:**
```
HSET ratelimit:leaky:user123
  volume     3.0
  last_leak  1751673600
```

**Lua Script Highlights:**
```lua
-- Drain the bucket
local elapsed = math.max(0, now - last_leak)
volume = math.max(0, volume - (elapsed * leak_rate))

-- Try to add
if volume + 1 <= capacity then
    volume = volume + 1
    allowed = 1
end
```

| Pros | Cons |
|------|------|
| Guarantees a smooth, constant output rate | No burst tolerance at all |
| Excellent for queue-based systems | Stricter than token bucket |
| Predictable server load | Can feel unfair to bursty clients |

---

### 4. Sliding Window Log

**Port:** `8084` &nbsp;|&nbsp; **Redis Key Pattern:** `ratelimit:swlog:{userId}` &nbsp;|&nbsp; **Redis Data Structure:** `ZSET` (Sorted Set)

```
  Window = 60s, Limit = 5

  Timeline:  t=10  t=20  t=30  t=55  t=58     t=62 (new request)
             ──────────────────────────────────────►

  At t=62, sliding window = [2, 62]
  ┌──────────────────────────────────────────────────┐
  │  ╳10  ╳20  ╳30              ■55   ■58            │
  │  (expired) (expired)        (in window)          │
  └──────────────────────────────────────────────────┘
  After pruning: {30, 55, 58} → count=3 → room for 2 more → ALLOWED ✅

  At t=62, if count was 5:
  After pruning: {10, 20, 30, 55, 58} → count=5 → at limit → DENIED ✋
```

**How it works:**
1. Every incoming request's timestamp is stored as an entry in a Redis **Sorted Set** (score = timestamp, member = unique ID).
2. On each request, the script removes all entries older than `now - window` using `ZREMRANGEBYSCORE`.
3. It counts the remaining entries with `ZCARD`.
4. If `count < limit`, the request is added and allowed. Otherwise, denied.
5. The key auto-expires after `window` seconds.

**Why Sorted Set?** Redis Sorted Sets allow efficient range removal (`ZREMRANGEBYSCORE`) and counting (`ZCARD`) — both O(log N) — making them perfect for time-based sliding windows.

**Configuration** (`application.yaml`):
```yaml
ratelimit:
  sliding-window-log:
    limit: 5               # Max requests in the sliding window
    window-seconds: 60      # Window duration in seconds
```

**Redis Storage:**
```
ZADD ratelimit:swlog:user123
  1751673610  "1751673610-482917"
  1751673620  "1751673620-193744"
  1751673630  "1751673630-847261"
```

> Each member includes a random suffix (`now-random`) to handle multiple requests arriving in the same second — without this, the sorted set would de-duplicate same-second requests.

**Lua Script:**
```lua
local window_start = now - window
redis.call('ZREMRANGEBYSCORE', key, '-inf', window_start)
local count = redis.call('ZCARD', key)

if count < limit then
    redis.call('ZADD', key, now, now .. '-' .. math.random(1000000))
    redis.call('EXPIRE', key, window)
    return {1, limit - count - 1, window}
else
    local ttl = redis.call('TTL', key)
    if ttl < 0 then ttl = window end
    return {0, 0, ttl}
end
```

| Pros | Cons |
|------|------|
| **Exact** precision — no boundary burst at all | High memory: stores every request timestamp |
| Smoothly sliding window, no edge effects | O(N) memory per user (N = limit) |
| Simple to reason about | `ZREMRANGEBYSCORE` is O(log N + M) per call |
| Perfect for low-volume, high-accuracy needs | Not ideal for very high request limits (e.g., 10K/min) |

---

### 5. Sliding Window Counter

**Port:** `8085` &nbsp;|&nbsp; **Redis Key Pattern:** `ratelimit:swcnt:{userId}:{windowStart}` &nbsp;|&nbsp; **Redis Data Structure:** `STRING` (two counters)

```
  Window = 60s, Limit = 5

  Previous Window [0–60]      Current Window [60–120]
  ┌────────────────────┐      ┌────────────────────┐
  │  count = 3         │      │  count = 2         │
  └────────────────────┘      └────────────────────┘
                                     ▲
                                     │ t=75 (25% into current window)
                                     │
  weighted = prev × (1 - 0.25) + current
           = 3 × 0.75 + 2
           = 2.25 + 2 = 4.25
  4.25 < 5 → ALLOWED ✅

  ─────────────────────────────────────────────────

  Same scenario but current count = 5:
  weighted = 3 × 0.75 + 5 = 7.25
  7.25 > 5 → DENIED ✋
```

**How it works:**
1. Time is divided into fixed windows (like Fixed Window Counter).
2. The algorithm maintains **two counters**: one for the **current window** and one for the **previous window**.
3. On each request, it calculates a **weighted count** by blending both:
   - `weight = 1 - (time elapsed in current window / window size)`
   - `weighted = previous_count × weight + current_count`
4. If `weighted < limit`, the current window's counter is incremented and the request is allowed.
5. Previous window keys expire after `2 × window` seconds (available for blending during the next window).

**Why two counters?** This is the key insight — instead of storing every timestamp (like Sliding Window Log), we approximate the sliding window using just two integers. This gives us **near-perfect accuracy with O(1) memory per user**.

**Configuration** (`application.yaml`):
```yaml
ratelimit:
  sliding-window-counter:
    limit: 5               # Max requests in the sliding window
    window-seconds: 60      # Window duration in seconds
```

**Redis Storage:**
```
SET ratelimit:swcnt:user123:1751673600  "3"   # previous window count
SET ratelimit:swcnt:user123:1751673660  "2"   # current window count
```

**Lua Script:**
```lua
-- Determine window boundaries
local current_window = math.floor(now / window) * window
local prev_window = current_window - window

-- Build keys
local curr_key = key .. ':' .. current_window
local prev_key = key .. ':' .. prev_window

-- Get counts
local prev_count = tonumber(redis.call('GET', prev_key) or '0') or 0
local curr_count = tonumber(redis.call('GET', curr_key) or '0') or 0

-- Calculate weighted count
local elapsed_ratio = (now - current_window) / window
local weight = 1 - elapsed_ratio
local weighted_count = prev_count * weight + curr_count

if weighted_count < limit then
    redis.call('INCR', curr_key)
    redis.call('EXPIRE', curr_key, window * 2)
    return {1, math.floor(limit - weighted_count - 1), current_window + window - now}
else
    return {0, 0, current_window + window - now}
end
```

| Pros | Cons |
|------|------|
| **O(1) memory** — only 2 counters per user | Approximate (not exact like Log) |
| Eliminates most of the boundary burst problem | Slight over/under counting near window edges |
| Best balance of accuracy vs. memory | Slightly more complex logic than fixed window |
| Scales to very high request limits | Weighted calculation involves floating point |

---

## 📊 Algorithm Comparison Matrix

| Feature | Fixed Window | Token Bucket | Leaky Bucket | Sliding Window Log | Sliding Window Counter |
|---------|:----------:|:----------:|:----------:|:----------:|:----------:|
| **Port** | 8081 | 8082 | 8083 | 8084 | 8085 |
| **Redis Type** | STRING | HASH | HASH | ZSET | 2× STRING |
| **Memory/User** | 1 key | 1 hash (2 fields) | 1 hash (2 fields) | 1 sorted set (N entries) | 2 keys |
| **Precision** | Low | Medium | High | **Exact** | Approximate |
| **Burst Handling** | 2× at edges | Controlled bursts | No bursts | Smooth | Mostly smooth |
| **Boundary Problem** | ⚠️ Yes | ✅ No | ✅ No | ✅ No | ✅ Minimal |
| **Time Complexity** | O(1) | O(1) | O(1) | O(log N) | O(1) |
| **Space Complexity** | O(1) | O(1) | O(1) | O(N) | O(1) |
| **Best For** | Simple APIs, internal tools | Most APIs (industry standard) | Queue systems, strict rate output | Low-volume, high-accuracy | High-volume APIs |
| **Used By** | Basic implementations | AWS, Stripe, GitHub | Nginx, network routers | Logging systems | Cloudflare, Redis Labs |

### When to Use Which?

```
                     Need burst tolerance?
                    /                     \
                  YES                      NO
                  /                         \
        Token Bucket              Need constant output rate?
                                  /                        \
                                YES                         NO
                                /                            \
                        Leaky Bucket              Need exact precision?
                                                  /                   \
                                                YES                    NO
                                                /                       \
                                    Sliding Window Log        Memory constrained?
                                                              /               \
                                                            YES                NO
                                                            /                   \
                                            Fixed Window Counter    Sliding Window Counter
```

---

## ⚙️ Configuration Reference

All configuration is in each module's `application.yaml`. Override via environment variables or Spring profiles.

| Module                   | Port   | Properties                                                        | Defaults    |
|--------------------------|--------|-------------------------------------------------------------------|-------------|
| `fixed-window`           | `8081` | `ratelimit.fixed-window.limit`, `.window-seconds`                 | `5`, `60`   |
| `token-bucket`           | `8082` | `ratelimit.token-bucket.capacity`, `.refill-rate`                 | `5`, `1`    |
| `leaky-bucket`           | `8083` | `ratelimit.leaky-bucket.capacity`, `.leak-rate`                   | `5`, `1`    |
| `sliding-window-log`     | `8084` | `ratelimit.sliding-window-log.limit`, `.window-seconds`           | `5`, `60`   |
| `sliding-window-counter` | `8085` | `ratelimit.sliding-window-counter.limit`, `.window-seconds`       | `5`, `60`   |

**Redis connection** (all modules):
```yaml
spring:
  data:
    redis:
      host: localhost
      port: 6379
```

---

## 🔗 Shared Module: `common`

The `common` module is the backbone shared across all algorithm modules. Here's what each component does:

### `RateLimitResult` — Unified Response DTO
```java
public record RateLimitResult(
    boolean allowed,          // Was the request allowed?
    long remaining,           // How many requests/tokens are left?
    long resetAtEpochSeconds  // When does the limit reset? (epoch seconds)
) {}
```

### `LuaScriptLoader` — Generic Lua Script Loader
Loads Lua scripts from the classpath and wraps them in Spring's `RedisScript` for atomic execution:
```java
public static <T> RedisScript<T> load(String classPathLocation, Class<T> resultType) {
    DefaultRedisScript<T> script = new DefaultRedisScript<>();
    script.setLocation(new ClassPathResource(classPathLocation));
    script.setResultType(resultType);
    return script;
}
```

### `TimeProvider` — Clock Abstraction
An interface that abstracts the system clock. Production code uses `SystemTimeProvider` (returns `Instant.now().getEpochSecond()`), while unit tests can inject a mock to control time:
```java
public interface TimeProvider {
    long nowEpochSecond();
}
```

---

## 🧪 Testing

### Run Unit Tests

```bash
# All modules
./gradlew test

# Specific module
./gradlew :token-bucket:test
./gradlew :sliding-window-log:test
```

### Manual Testing

```bash
# 1. Start Redis
docker-compose up -d

# 2. Run the desired module (e.g., sliding-window-log)
./gradlew :sliding-window-log:bootRun

# 3. Send requests
curl "http://localhost:8084/api/check?userId=testUser"

# 4. Observe 429 after exceeding the limit
for i in $(seq 1 10); do
  echo "Request $i: $(curl -s -o /dev/null -w '%{http_code}' 'http://localhost:8084/api/check?userId=testUser')"
done
```

Expected output:
```
Request 1: 200
Request 2: 200
Request 3: 200
Request 4: 200
Request 5: 200
Request 6: 429
Request 7: 429
...
```

### Compare All Algorithms Side by Side

Run all five services simultaneously and hit them with the same traffic:

```bash
# Terminal 1–5: Start all services
./gradlew :fixed-window:bootRun &
./gradlew :token-bucket:bootRun &
./gradlew :leaky-bucket:bootRun &
./gradlew :sliding-window-log:bootRun &
./gradlew :sliding-window-counter:bootRun &

# Send 10 requests to each
for port in 8081 8082 8083 8084 8085; do
  echo "--- Port $port ---"
  for i in $(seq 1 10); do
    echo "  Request $i: $(curl -s -o /dev/null -w '%{http_code}' "http://localhost:$port/api/check?userId=testUser")"
  done
done
```

---

## 🔑 Design Decisions

| Decision | Rationale |
|----------|-----------|
| **Lua scripts for atomicity** | All rate limit logic runs as a single atomic operation in Redis — no race conditions, no distributed locks needed |
| **Time passed from Java, not `redis.call('TIME')`** | `TIME` is non-deterministic and breaks Redis replication; passing time from the application keeps scripts deterministic |
| **Separate Spring Boot apps per algorithm** | Each runs independently for comparison/benchmarking; easily swappable in production behind a gateway |
| **`TimeProvider` interface** | Abstracts system clock for unit testing without real Redis |
| **Auto-expiring keys** | Stale keys are cleaned up automatically via `EXPIRE`, preventing memory leaks |
| **Sorted Set for Sliding Window Log** | Enables efficient `ZREMRANGEBYSCORE` for pruning + `ZCARD` for counting in O(log N) |
| **Two STRING counters for Sliding Window Counter** | O(1) memory approximation instead of O(N) — ideal for high-traffic production use |

---

## 🗺️ Roadmap

- [x] **Fixed Window Counter** — Simple time-window based counting
- [x] **Token Bucket** — Burst-friendly with steady refill
- [x] **Leaky Bucket** — Smooth, constant-rate output
- [x] **Sliding Window Log** — Exact precision with sorted set timestamps
- [x] **Sliding Window Counter** — Memory-efficient weighted approximation

---

## 📄 License

This project is for educational and reference purposes.
