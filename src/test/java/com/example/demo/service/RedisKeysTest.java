package com.example.demo.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RedisKeysTest {

    @Test
    void orderQueue_generatesCorrectKey() {
        String key = RedisKeys.orderQueue("store123");
        assertEquals("order:queue:store123", key);
    }

    @Test
    void pickerQueue_generatesCorrectKey() {
        String key = RedisKeys.pickerQueue("store123");
        assertEquals("picker:queue:store123", key);
    }

    @Test
    void orderScoreMeta_generatesCorrectKey() {
        String key = RedisKeys.orderScoreMeta("order456");
        assertEquals("order:scoremeta:order456", key);
    }

    @Test
    void pickerScoreMeta_generatesCorrectKey() {
        String key = RedisKeys.pickerScoreMeta("picker789");
        assertEquals("picker:scoremeta:picker789", key);
    }

    @Test
    void allocationHistory_generatesCorrectKey() {
        String key = RedisKeys.allocationHistory("store123");
        assertEquals("allocation:history:store123", key);
    }
}

