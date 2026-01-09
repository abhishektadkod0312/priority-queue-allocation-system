# Order Allocation System

A Spring Boot 4 / Java 25 application for real-time order-to-picker allocation using Redis.

## Problem Solved

Replaces batch cron-based allocation with event-driven, single-order allocation when a picker becomes available. This eliminates allocation delay, burst load, and suboptimal matching.

## Architecture

### Data Structures (Per Store)

| Structure | Redis Type | Key Pattern | Purpose |
|-----------|------------|-------------|---------|
| Orders Queue | ZSET | `order:queue:{storeId}` | Priority queue for pending orders |
| Pickers Queue | ZSET | `picker:queue:{storeId}` | Priority queue for available pickers |
| Order Score Meta | HASH | `order:scoremeta:{orderId}` | Score breakdown for debugging (24h TTL) |
| Picker Score Meta | HASH | `picker:scoremeta:{pickerId}` | Score breakdown for debugging (24h TTL) |

**Note:** Lower score = higher priority

### Queue Triggers

**Order Queue Inserts:**
- Order confirmed
- Order crosses OAT (Optimal Allocation Time)

**Picker Queue Inserts:**
- Picker logs in
- Picker finishes an order
- Picker becomes available after role change

### Priority Calculation

**Order Priority Score:**
```
score = w1 * normalized(now - OAT) 
      + w2 * normalized(PRIORITY_ORDER) 
      + w3 * normalized(SKU_COUNT)
```

**Picker Priority Score:**
```
score = w1 * normalized(SKU_COMPLETED) 
      + w2 * normalized(ORDER_COMPLETED)
```

Inputs are normalized to [0, 1] range before weighting.

### Allocation Logic

When both queues are non-empty:
1. Pop top order (lowest score) and top picker (lowest score)
2. Allocate exactly one order to one picker
3. Operation is executed atomically using a Redis Lua script

## Configuration

```properties
# Redis Configuration
spring.data.redis.host=localhost
spring.data.redis.port=6379

# Order Weights (must sum to 1.0)
allocation.weights.order.oat-delta=0.4
allocation.weights.order.priority=0.35
allocation.weights.order.sku-count=0.25

# Picker Weights (must sum to 1.0)
allocation.weights.picker.sku-completed=0.5
allocation.weights.picker.order-completed=0.5

# Score metadata TTL
allocation.score-meta-ttl-hours=24
```

## API Endpoints

### Enqueue Order
```bash
POST /api/v1/allocation/orders
Content-Type: application/json

{
  "orderId": "order-123",
  "storeId": "store-001",
  "oatTimestamp": 1704808800000,  # Optional, defaults to now
  "priorityOrder": 1,              # 1-10, lower = higher priority
  "skuCount": 5
}
```

### Enqueue Picker
```bash
POST /api/v1/allocation/pickers
Content-Type: application/json

{
  "pickerId": "picker-456",
  "storeId": "store-001",
  "skuCompleted": 1000,
  "orderCompleted": 100
}
```

### Trigger Allocation Manually
```bash
POST /api/v1/allocation/trigger/{storeId}
```

### Get Queue Status
```bash
GET /api/v1/allocation/status/{storeId}
```

Response:
```json
{
  "storeId": "store-001",
  "orderQueueSize": 5,
  "pickerQueueSize": 3,
  "canAllocate": true,
  "topOrderId": "order-123",
  "topPickerId": "picker-456",
  "topOrderScoreMeta": {
    "oatDelta": -5,
    "initialPriority": 1,
    "skuScore": 5,
    "finalScore": 0.234
  },
  "topPickerScoreMeta": {
    "skuCompletedScore": 1000,
    "orderCompletedScore": 100,
    "finalScore": 0.45
  }
}
```

### Remove Order from Queue
```bash
DELETE /api/v1/allocation/orders/{storeId}/{orderId}
```

### Remove Picker from Queue
```bash
DELETE /api/v1/allocation/pickers/{storeId}/{pickerId}
```

### Get Order Score Metadata
```bash
GET /api/v1/allocation/orders/{orderId}/score
```

### Get Picker Score Metadata
```bash
GET /api/v1/allocation/pickers/{pickerId}/score
```

## Running the Application

### Prerequisites
- Java 25
- Redis server running on localhost:6379

### Build and Run
```bash
./gradlew build
./gradlew bootRun
```

### Run Tests
```bash
./gradlew test
```

## Project Structure

```
src/main/java/com/example/demo/
├── config/
│   ├── AllocationProperties.java    # Configuration properties
│   └── RedisConfig.java             # Redis template and Lua script config
├── controller/
│   ├── AllocationController.java    # REST API endpoints
│   └── GlobalExceptionHandler.java  # Exception handling
├── dto/
│   ├── AllocationResponse.java      # Allocation result DTO
│   ├── OrderRequest.java            # Order enqueue request DTO
│   ├── PickerRequest.java           # Picker enqueue request DTO
│   └── QueueStatusResponse.java     # Queue status DTO
├── model/
│   ├── Allocation.java              # Allocation record
│   ├── Order.java                   # Order record
│   ├── OrderScoreMeta.java          # Order score metadata
│   ├── Picker.java                  # Picker record
│   └── PickerScoreMeta.java         # Picker score metadata
├── service/
│   ├── AllocationService.java       # Atomic allocation logic
│   ├── QueueService.java            # Queue operations
│   ├── RedisKeys.java               # Redis key templates
│   └── ScoreCalculationService.java # Priority score calculation
└── DemoApplication.java             # Application entry point

src/main/resources/
├── application.properties           # Configuration
└── scripts/
    └── allocate.lua                 # Atomic allocation Lua script
```

## Key Design Decisions

1. **Atomic Operations**: Redis Lua script ensures allocation is atomic (no race conditions)
2. **Per-Store Isolation**: Each store has its own queues
3. **No Business Logic in Lua**: Lua script only handles pop operations
4. **Event-Driven**: Allocation triggered on queue insert
5. **Score Explainability**: Score breakdown stored for debugging

