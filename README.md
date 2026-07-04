# 🚦 Rate Limiting Algorithms

A collection of **production-grade rate limiting algorithms** implemented as independent Spring Boot microservices, backed by **Redis** and **Lua scripting** for atomic, high-performance request throttling.

> **Status:** 3 of 5 planned algorithms are implemented and fully working. The remaining two (Sliding Window Log & Sliding Window Counter) are coming soon.

---

## 📐 Architecture Overview

```
┌──────────────────────────────────────────────────────────┐
│                    Gradle Multi-Module                    │
│                                                          │
│  ┌──────────┐   ┌──────────────┐   ┌──────────────┐     │
│  │  common   │◄──│ fixed-window │   │ token-bucket │     │
│  │ (shared)  │◄──│   :8081      │   │   :8082      │     │
│  │          │◄──│              │   │              │     │
│  └──────────┘   └──────┬───────┘   └──────┬───────┘     │
│        ▲               │                  │              │
│        │         ┌─────┴──────────────────┘              │
│        │         │                                       │
│  ┌─────┴────┐    │    ┌──────────────┐                   │
│  │          │    └────│ leaky-bucket │                   │
│  │          │         │   :8083      │                   │
│  │          │         └──────┬───────┘                   │
│  └──────────┘                │                           │
└──────────────────────────────┼───────────────────────────┘
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
├── common/                          # Shared library module
│   └── src/main/java/.../common/
│       ├── config/RedisConfig.java       # StringRedisTemplate bean
│       ├── dto/RateLimitResult.java      # Unified response record
│       ├── exception/RateLimitExceededException.java
│       ├── lua/LuaScriptLoader.java      # Generic Lua script loader
│       └── util/
│           ├── TimeProvider.java         # Interface for testability
│           └── SystemTimeProvider.java   # Production implementation
│
├── fixed-window/                    # Fixed Window Counter  (port 8081)
│   └── src/main/
│       ├── java/.../fixed_window/
│       │   ├── config/RedisScriptConfig.java
│       │   ├── controller/RateLimitController.java
│       │   └── service/FixedWindowRateLimiterService.java
│       └── resources/
│           ├── lua/fixed_window.lua
│           └── application.yaml
│
├── token-bucket/                    # Token Bucket           (port 8082)
│   └── src/main/
│       ├── java/.../token_bucket/
│       │   ├── config/RedisScriptConfig.java
│       │   ├── controller/RateLimitController.java
│       │   └── service/TokenBucketRateLimiterService.java
│       └── resources/
│           ├── lua/token-bucket.lua
│           └── application.yaml
│
├── leaky-bucket/                    # Leaky Bucket           (port 8083)
│   └── src/main/
│       ├── java/.../leaky_bucket/
│       │   ├── config/RedisScriptConfig.java
│       │   ├── controller/leakyController.java
│       │   └── service/LeakeyBucketAlgoService.java
│       └── resources/
│           ├── lua/leaky_bucket.lua
│           └── application.yaml
│
├── build.gradle                     # Root build (plugins + subproject config)
├── settings.gradle                  # Module includes
├── docker-compose.yml               # Redis container
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
```

---

## 🔌 API Reference

All three modules expose the **same REST endpoint** with an identical contract:

### `GET /api/check`

| Parameter | Type   | Required | Description          |
|-----------|--------|----------|----------------------|
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

**Port:** `8081` &nbsp;|&nbsp; **Redis Key Pattern:** `ratelimit:fixed:{userId}`

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
-- Returns: {allowed, currentCount, ttl}
```

| Pros | Cons |
|------|------|
| Simple to understand & implement | Boundary burst problem (2× burst at window edges) |
| Very low memory footprint | Not smooth — bursty traffic pattern |
| O(1) per request | No partial credit between windows |

---

### 2. Token Bucket

**Port:** `8082` &nbsp;|&nbsp; **Redis Key Pattern:** `ratelimit:token:{userId}`

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

| Pros | Cons |
|------|------|
| Allows controlled bursts up to capacity | Slightly more complex than fixed window |
| Smooth, natural traffic shaping | Two fields per key in Redis |
| Industry standard (AWS, Stripe use this) | Floating point arithmetic in Lua |

---

### 3. Leaky Bucket

**Port:** `8083` &nbsp;|&nbsp; **Redis Key Pattern:** `ratelimit:leaky:{userId}`

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

| Pros | Cons |
|------|------|
| Guarantees a smooth, constant output rate | No burst tolerance at all |
| Excellent for queue-based systems | Stricter than token bucket |
| Predictable server load | Can feel unfair to bursty clients |

---

## ⚙️ Configuration Reference

All configuration is in each module's `application.yaml`. Override via environment variables or Spring profiles.

| Module        | Port   | Properties                                            | Defaults      |
|---------------|--------|-------------------------------------------------------|---------------|
| fixed-window  | `8081` | `ratelimit.fixed-window.limit`, `.window-seconds`     | `5`, `60`     |
| token-bucket  | `8082` | `ratelimit.token-bucket.capacity`, `.refill-rate`     | `5`, `1`      |
| leaky-bucket  | `8083` | `ratelimit.leaky-bucket.capacity`, `.leak-rate`       | `5`, `1`      |

**Redis connection** (all modules):
```yaml
spring:
  data:
    redis:
      host: localhost
      port: 6379
```

---

## 🧪 Testing

### Run Unit Tests

```bash
./gradlew test
```

### Manual Testing

```bash
# 1. Start Redis
docker-compose up -d

# 2. Run the desired module (e.g., token-bucket)
./gradlew :token-bucket:bootRun

# 3. Send requests
curl "http://localhost:8082/api/check?userId=testUser"

# 4. Observe 429 after exceeding the limit
for i in $(seq 1 10); do
  echo "Request $i: $(curl -s -o /dev/null -w '%{http_code}' 'http://localhost:8082/api/check?userId=testUser')"
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

---

## 🗺️ Roadmap

- [x] **Fixed Window Counter** — Simple time-window based counting
- [x] **Token Bucket** — Burst-friendly with steady refill
- [x] **Leaky Bucket** — Smooth, constant-rate output
- [ ] **Sliding Window Log** — *Coming soon*
- [ ] **Sliding Window Counter** — *Coming soon*

---

## 🔑 Design Decisions

| Decision | Rationale |
|----------|-----------|
| **Lua scripts for atomicity** | All rate limit logic runs as a single atomic operation in Redis — no race conditions, no distributed locks needed |
| **Time passed from Java, not `redis.call('TIME')`** | `TIME` is non-deterministic and breaks Redis replication; passing time from the application keeps scripts deterministic |
| **Separate Spring Boot apps per algorithm** | Each runs independently for comparison/benchmarking; easily swappable in production behind a gateway |
| **`TimeProvider` interface** | Abstracts system clock for unit testing without real Redis |
| **Auto-expiring keys** | Stale keys are cleaned up automatically via `EXPIRE`, preventing memory leaks |

---

## 📄 License

This project is for educational and reference purposes.
