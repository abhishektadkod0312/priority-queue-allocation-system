package com.example.demo.model;

/**
 * Score metadata for debugging and ops visibility.
 * Stored in Redis HASH with 24h TTL.
 */
public record OrderScoreMeta(
    double oatDelta,
    double initialPriority,
    double skuScore,
    double finalScore
) {}

