package com.example.demo.service;

import com.example.demo.config.AllocationProperties;
import com.example.demo.model.Order;
import com.example.demo.model.OrderScoreMeta;
import com.example.demo.model.Picker;
import com.example.demo.model.PickerScoreMeta;
import org.springframework.stereotype.Service;

import java.time.Instant;

/**
 * Service for calculating priority scores for orders and pickers.
 * Lower score = higher priority.
 */
@Service
public class ScoreCalculationService {

    private final AllocationProperties properties;

    // Normalization constants (configurable based on domain)
    private static final double MAX_OAT_DELTA_MINUTES = 120.0;  // 2 hours max delay
    private static final double MAX_PRIORITY_ORDER = 10.0;       // Priority levels 1-10
    private static final double MAX_SKU_COUNT = 100.0;           // Max SKUs per order
    private static final double MAX_SKU_COMPLETED = 10000.0;     // Experienced picker threshold
    private static final double MAX_ORDER_COMPLETED = 1000.0;    // Experienced picker threshold

    public ScoreCalculationService(AllocationProperties properties) {
        this.properties = properties;
    }

    /**
     * Calculate order priority score.
     * score = w1 * normalized(now - OAT) + w2 * normalized(PRIORITY_ORDER) + w3 * normalized(SKU_COUNT)
     * Lower score = higher priority (orders that have waited longer get lower scores)
     */
    public double calculateOrderScore(Order order) {
        var weights = properties.getWeights().getOrder();

        // Calculate OAT delta (negative means order has been waiting)
        long oatDeltaMinutes = java.time.Duration.between(order.oat(), Instant.now()).toMinutes();

        // Normalize: orders waiting longer get lower (better) scores
        // Invert the delta so waiting longer = lower score
        double normalizedOatDelta = normalize(-oatDeltaMinutes, -MAX_OAT_DELTA_MINUTES, MAX_OAT_DELTA_MINUTES);

        // Lower priority order number = higher priority = lower score
        double normalizedPriority = normalize(order.priorityOrder(), 1, MAX_PRIORITY_ORDER);

        // Fewer SKUs = faster to pick = lower score (prioritize quick orders)
        double normalizedSkuCount = normalize(order.skuCount(), 1, MAX_SKU_COUNT);

        return weights.getOatDelta() * normalizedOatDelta
             + weights.getPriority() * normalizedPriority
             + weights.getSkuCount() * normalizedSkuCount;
    }

    /**
     * Get order score metadata for debugging.
     */
    public OrderScoreMeta getOrderScoreMeta(Order order) {
        var weights = properties.getWeights().getOrder();

        long oatDeltaMinutes = java.time.Duration.between(order.oat(), Instant.now()).toMinutes();
        double normalizedOatDelta = normalize(-oatDeltaMinutes, -MAX_OAT_DELTA_MINUTES, MAX_OAT_DELTA_MINUTES);
        double normalizedPriority = normalize(order.priorityOrder(), 1, MAX_PRIORITY_ORDER);
        double normalizedSkuCount = normalize(order.skuCount(), 1, MAX_SKU_COUNT);

        double finalScore = weights.getOatDelta() * normalizedOatDelta
                          + weights.getPriority() * normalizedPriority
                          + weights.getSkuCount() * normalizedSkuCount;

        return new OrderScoreMeta(
            oatDeltaMinutes,
            order.priorityOrder(),
            order.skuCount(),
            finalScore
        );
    }

    /**
     * Calculate picker priority score.
     * score = w1 * normalized(SKU_COMPLETED) + w2 * normalized(ORDER_COMPLETED)
     * Lower score = higher priority (more experienced pickers get lower scores)
     */
    public double calculatePickerScore(Picker picker) {
        var weights = properties.getWeights().getPicker();

        // More experience = lower score (prioritize experienced pickers)
        // Invert so higher completion = lower score
        double normalizedSkuCompleted = 1.0 - normalize(picker.skuCompleted(), 0, MAX_SKU_COMPLETED);
        double normalizedOrderCompleted = 1.0 - normalize(picker.orderCompleted(), 0, MAX_ORDER_COMPLETED);

        return weights.getSkuCompleted() * normalizedSkuCompleted
             + weights.getOrderCompleted() * normalizedOrderCompleted;
    }

    /**
     * Get picker score metadata for debugging.
     */
    public PickerScoreMeta getPickerScoreMeta(Picker picker) {
        var weights = properties.getWeights().getPicker();

        double normalizedSkuCompleted = 1.0 - normalize(picker.skuCompleted(), 0, MAX_SKU_COMPLETED);
        double normalizedOrderCompleted = 1.0 - normalize(picker.orderCompleted(), 0, MAX_ORDER_COMPLETED);

        double finalScore = weights.getSkuCompleted() * normalizedSkuCompleted
                          + weights.getOrderCompleted() * normalizedOrderCompleted;

        return new PickerScoreMeta(
            picker.skuCompleted(),
            picker.orderCompleted(),
            finalScore
        );
    }

    /**
     * Normalize a value to range [0, 1].
     */
    private double normalize(double value, double min, double max) {
        if (max == min) return 0.5;
        double normalized = (value - min) / (max - min);
        return Math.max(0.0, Math.min(1.0, normalized));
    }
}

