package com.example.demo.model;

/**
 * Represents a picker in the system.
 */
public record Picker(
    String pickerId,
    String storeId,
    int skuCompleted,      // Total SKUs picked
    int orderCompleted     // Total orders completed
) {}

