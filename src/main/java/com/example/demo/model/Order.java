package com.example.demo.model;

import java.time.Instant;

/**
 * Represents an order in the system.
 */
public record Order(
    String orderId,
    String storeId,
    Instant oat,           // Optimal Allocation Time
    int priorityOrder,     // Business priority (lower = higher priority)
    int skuCount           // Number of SKUs in the order
) {}

