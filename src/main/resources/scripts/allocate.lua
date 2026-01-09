-- Atomic allocation Lua script
-- Pops the top order and top picker from their respective queues atomically
-- KEYS[1] = order queue key (ZSET)
-- KEYS[2] = picker queue key (ZSET)
-- Returns: {orderId, pickerId} or empty array if either queue is empty

local orderQueueKey = KEYS[1]
local pickerQueueKey = KEYS[2]

-- Get top order (lowest score = highest priority)
local topOrder = redis.call('ZRANGE', orderQueueKey, 0, 0)
if #topOrder == 0 then
    return {}
end

-- Get top picker (lowest score = highest priority)
local topPicker = redis.call('ZRANGE', pickerQueueKey, 0, 0)
if #topPicker == 0 then
    return {}
end

local orderId = topOrder[1]
local pickerId = topPicker[1]

-- Remove both from their queues atomically
redis.call('ZREM', orderQueueKey, orderId)
redis.call('ZREM', pickerQueueKey, pickerId)

return {orderId, pickerId}

