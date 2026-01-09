package com.example.demo.dto;

/**
 * Response DTO for allocation result.
 */
public record AllocationResponse(
    String orderId,
    String pickerId,
    String storeId,
    long timestamp,
    boolean success,
    String message
) {
    public static AllocationResponse success(String orderId, String pickerId, String storeId, long timestamp) {
        return new AllocationResponse(orderId, pickerId, storeId, timestamp, true, "Order allocated successfully");
    }

    public static AllocationResponse noAllocation(String storeId) {
        return new AllocationResponse(null, null, storeId, 0, false, "No allocation possible - queues may be empty");
    }
}

