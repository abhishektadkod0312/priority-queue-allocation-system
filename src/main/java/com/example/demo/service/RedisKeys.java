package com.example.demo.service;

/**
 * Redis key templates for the allocation system.
 */
public final class RedisKeys {

    private RedisKeys() {
        // Utility class
    }

    /**
     * Orders queue key: ZSET order:queue:{storeId}
     */
    public static String orderQueue(String storeId) {
        return "order:queue:" + storeId;
    }

    /**
     * Pickers queue key: ZSET picker:queue:{storeId}
     */
    public static String pickerQueue(String storeId) {
        return "picker:queue:" + storeId;
    }

    /**
     * Order score metadata key: HASH order:scoremeta:{orderId}
     */
    public static String orderScoreMeta(String orderId) {
        return "order:scoremeta:" + orderId;
    }

    /**
     * Picker score metadata key: HASH picker:scoremeta:{pickerId}
     */
    public static String pickerScoreMeta(String pickerId) {
        return "picker:scoremeta:" + pickerId;
    }

    /**
     * Allocation history key: LIST allocation:history:{storeId}
     */
    public static String allocationHistory(String storeId) {
        return "allocation:history:" + storeId;
    }
}

