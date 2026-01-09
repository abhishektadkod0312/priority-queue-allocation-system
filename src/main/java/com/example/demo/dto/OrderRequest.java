package com.example.demo.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

/**
 * Request DTO for adding an order to the queue.
 */
public record OrderRequest(
    @NotBlank(message = "Order ID is required")
    String orderId,

    @NotBlank(message = "Store ID is required")
    String storeId,

    Long oatTimestamp,  // Optimal Allocation Time as epoch millis (null = now)

    @Min(value = 1, message = "Priority must be at least 1")
    int priorityOrder,

    @Min(value = 1, message = "SKU count must be at least 1")
    int skuCount
) {}

