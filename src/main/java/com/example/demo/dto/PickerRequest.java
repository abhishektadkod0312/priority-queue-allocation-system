package com.example.demo.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

/**
 * Request DTO for adding a picker to the queue.
 */
public record PickerRequest(
    @NotBlank(message = "Picker ID is required")
    String pickerId,

    @NotBlank(message = "Store ID is required")
    String storeId,

    @Min(value = 0, message = "SKU completed cannot be negative")
    int skuCompleted,

    @Min(value = 0, message = "Order completed cannot be negative")
    int orderCompleted
) {}

