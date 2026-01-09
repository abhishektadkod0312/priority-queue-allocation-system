package com.example.demo.service;

import com.example.demo.model.Allocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Service for allocating orders to pickers atomically using Redis Lua script.
 */
@Service
public class AllocationService {

    private static final Logger log = LoggerFactory.getLogger(AllocationService.class);

    private final RedisTemplate<String, Object> redisTemplate;
    private final RedisScript<List> allocationScript;
    private final QueueService queueService;

    public AllocationService(
            RedisTemplate<String, Object> redisTemplate,
            RedisScript<List> allocationScript,
            QueueService queueService) {
        this.redisTemplate = redisTemplate;
        this.allocationScript = allocationScript;
        this.queueService = queueService;
    }

    /**
     * Try to allocate one order to one picker atomically.
     * This is the core allocation method that uses a Lua script for atomicity.
     *
     * @param storeId The store to allocate in
     * @return Optional containing the allocation if successful, empty otherwise
     */
    public Optional<Allocation> tryAllocate(String storeId) {
        String orderQueueKey = RedisKeys.orderQueue(storeId);
        String pickerQueueKey = RedisKeys.pickerQueue(storeId);

        // Execute atomic allocation using Lua script
        List<String> keys = List.of(orderQueueKey, pickerQueueKey);

        @SuppressWarnings("unchecked")
        List<Object> result = redisTemplate.execute(allocationScript, keys);

        if (result == null || result.isEmpty() || result.get(0) == null) {
            log.debug("No allocation possible for store {} - queues may be empty", storeId);
            return Optional.empty();
        }

        String orderId = result.get(0).toString();
        String pickerId = result.get(1).toString();
        long timestamp = System.currentTimeMillis();

        Allocation allocation = new Allocation(orderId, pickerId, storeId, timestamp);

        log.info("Allocated order {} to picker {} in store {}", orderId, pickerId, storeId);

        return Optional.of(allocation);
    }

    /**
     * Trigger allocation attempt for a store.
     * Called when a new order is added or a picker becomes available.
     */
    public Optional<Allocation> triggerAllocation(String storeId) {
        if (!queueService.canAllocate(storeId)) {
            log.debug("Cannot allocate for store {} - queues not ready", storeId);
            return Optional.empty();
        }

        return tryAllocate(storeId);
    }
}

