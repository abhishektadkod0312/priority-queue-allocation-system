package com.example.demo.model;

/**
 * Score metadata for picker debugging and ops visibility.
 */
public record PickerScoreMeta(
    double skuCompletedScore,
    double orderCompletedScore,
    double finalScore
) {}

