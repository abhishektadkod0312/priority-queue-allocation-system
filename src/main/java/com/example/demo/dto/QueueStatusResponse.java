package com.example.demo.dto;

import java.util.Map;

/**
 * Response DTO for queue status.
 */
public record QueueStatusResponse(
    String storeId,
    long orderQueueSize,
    long pickerQueueSize,
    boolean canAllocate,
    String topOrderId,
    String topPickerId,
    Map<Object, Object> topOrderScoreMeta,
    Map<Object, Object> topPickerScoreMeta
) {}

