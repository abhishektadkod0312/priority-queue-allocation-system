package com.example.demo.model;

/**
 * Represents an allocation of an order to a picker.
 */
public record Allocation(
    String orderId,
    String pickerId,
    String storeId,
    long timestamp
) {}

