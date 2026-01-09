package com.example.demo.service;

import com.example.demo.config.AllocationProperties;
import com.example.demo.model.Order;
import com.example.demo.model.OrderScoreMeta;
import com.example.demo.model.Picker;
import com.example.demo.model.PickerScoreMeta;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Service for managing order and picker queues in Redis.
 */
@Service
public class QueueService {

    private static final Logger log = LoggerFactory.getLogger(QueueService.class);

    private final RedisTemplate<String, Object> redisTemplate;
    private final ScoreCalculationService scoreCalculationService;
    private final AllocationProperties properties;

    public QueueService(
            RedisTemplate<String, Object> redisTemplate,
            ScoreCalculationService scoreCalculationService,
            AllocationProperties properties) {
        this.redisTemplate = redisTemplate;
        this.scoreCalculationService = scoreCalculationService;
        this.properties = properties;
    }

    /**
     * Add an order to the queue.
     * Called when: Order confirmed, Order crosses OAT
     */
    public void enqueueOrder(Order order) {
        String queueKey = RedisKeys.orderQueue(order.storeId());
        double score = scoreCalculationService.calculateOrderScore(order);

        // Add to sorted set
        redisTemplate.opsForZSet().add(queueKey, order.orderId(), score);

        // Store score metadata for debugging
        storeOrderScoreMeta(order);

        log.info("Order {} enqueued to store {} with score {}", order.orderId(), order.storeId(), score);
    }

    /**
     * Remove an order from the queue.
     */
    public void dequeueOrder(String storeId, String orderId) {
        String queueKey = RedisKeys.orderQueue(storeId);
        redisTemplate.opsForZSet().remove(queueKey, orderId);
        log.info("Order {} dequeued from store {}", orderId, storeId);
    }

    /**
     * Add a picker to the queue.
     * Called when: Picker logs in, Picker finishes an order, Picker becomes available after role change
     */
    public void enqueuePicker(Picker picker) {
        String queueKey = RedisKeys.pickerQueue(picker.storeId());
        double score = scoreCalculationService.calculatePickerScore(picker);

        // Add to sorted set
        redisTemplate.opsForZSet().add(queueKey, picker.pickerId(), score);

        // Store score metadata for debugging
        storePickerScoreMeta(picker);

        log.info("Picker {} enqueued to store {} with score {}", picker.pickerId(), picker.storeId(), score);
    }

    /**
     * Remove a picker from the queue.
     */
    public void dequeuePicker(String storeId, String pickerId) {
        String queueKey = RedisKeys.pickerQueue(storeId);
        redisTemplate.opsForZSet().remove(queueKey, pickerId);
        log.info("Picker {} dequeued from store {}", pickerId, storeId);
    }

    /**
     * Get the top order from the queue without removing.
     */
    public Optional<String> peekTopOrder(String storeId) {
        String queueKey = RedisKeys.orderQueue(storeId);
        Set<Object> result = redisTemplate.opsForZSet().range(queueKey, 0, 0);
        if (result == null || result.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(result.iterator().next().toString());
    }

    /**
     * Get the top picker from the queue without removing.
     */
    public Optional<String> peekTopPicker(String storeId) {
        String queueKey = RedisKeys.pickerQueue(storeId);
        Set<Object> result = redisTemplate.opsForZSet().range(queueKey, 0, 0);
        if (result == null || result.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(result.iterator().next().toString());
    }

    /**
     * Get order queue size for a store.
     */
    public long getOrderQueueSize(String storeId) {
        String queueKey = RedisKeys.orderQueue(storeId);
        Long size = redisTemplate.opsForZSet().size(queueKey);
        return size != null ? size : 0;
    }

    /**
     * Get picker queue size for a store.
     */
    public long getPickerQueueSize(String storeId) {
        String queueKey = RedisKeys.pickerQueue(storeId);
        Long size = redisTemplate.opsForZSet().size(queueKey);
        return size != null ? size : 0;
    }

    /**
     * Check if both queues have items (allocation possible).
     */
    public boolean canAllocate(String storeId) {
        return getOrderQueueSize(storeId) > 0 && getPickerQueueSize(storeId) > 0;
    }

    /**
     * Store order score metadata for debugging.
     */
    private void storeOrderScoreMeta(Order order) {
        String metaKey = RedisKeys.orderScoreMeta(order.orderId());
        OrderScoreMeta meta = scoreCalculationService.getOrderScoreMeta(order);

        Map<String, Object> metaMap = Map.of(
            "oatDelta", meta.oatDelta(),
            "initialPriority", meta.initialPriority(),
            "skuScore", meta.skuScore(),
            "finalScore", meta.finalScore()
        );

        redisTemplate.opsForHash().putAll(metaKey, metaMap);
        redisTemplate.expire(metaKey, Duration.ofHours(properties.getScoreMetaTtlHours()));
    }

    /**
     * Store picker score metadata for debugging.
     */
    private void storePickerScoreMeta(Picker picker) {
        String metaKey = RedisKeys.pickerScoreMeta(picker.pickerId());
        PickerScoreMeta meta = scoreCalculationService.getPickerScoreMeta(picker);

        Map<String, Object> metaMap = Map.of(
            "skuCompletedScore", meta.skuCompletedScore(),
            "orderCompletedScore", meta.orderCompletedScore(),
            "finalScore", meta.finalScore()
        );

        redisTemplate.opsForHash().putAll(metaKey, metaMap);
        redisTemplate.expire(metaKey, Duration.ofHours(properties.getScoreMetaTtlHours()));
    }

    /**
     * Get order score metadata.
     */
    public Map<Object, Object> getOrderScoreMeta(String orderId) {
        String metaKey = RedisKeys.orderScoreMeta(orderId);
        return redisTemplate.opsForHash().entries(metaKey);
    }

    /**
     * Get picker score metadata.
     */
    public Map<Object, Object> getPickerScoreMeta(String pickerId) {
        String metaKey = RedisKeys.pickerScoreMeta(pickerId);
        return redisTemplate.opsForHash().entries(metaKey);
    }
}

