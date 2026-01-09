package com.example.demo.service;

import com.example.demo.config.AllocationProperties;
import com.example.demo.model.Order;
import com.example.demo.model.OrderScoreMeta;
import com.example.demo.model.Picker;
import com.example.demo.model.PickerScoreMeta;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class ScoreCalculationServiceTest {

    private ScoreCalculationService service;
    private AllocationProperties properties;

    @BeforeEach
    void setUp() {
        properties = new AllocationProperties();
        // Use default weights
        properties.getWeights().getOrder().setOatDelta(0.4);
        properties.getWeights().getOrder().setPriority(0.35);
        properties.getWeights().getOrder().setSkuCount(0.25);
        properties.getWeights().getPicker().setSkuCompleted(0.5);
        properties.getWeights().getPicker().setOrderCompleted(0.5);

        service = new ScoreCalculationService(properties);
    }

    @Test
    void calculateOrderScore_higherPriorityOrderGetsLowerScore() {
        // Higher priority (lower priority number) should get lower score
        Order highPriorityOrder = new Order("order1", "store1", Instant.now(), 1, 10);
        Order lowPriorityOrder = new Order("order2", "store1", Instant.now(), 10, 10);

        double highPriorityScore = service.calculateOrderScore(highPriorityOrder);
        double lowPriorityScore = service.calculateOrderScore(lowPriorityOrder);

        assertTrue(highPriorityScore < lowPriorityScore,
            "Higher priority order should have lower score");
    }

    @Test
    void calculateOrderScore_olderOrderGetsLowerScore() {
        // Order that has waited longer (older OAT) should get lower score
        Order olderOrder = new Order("order1", "store1", Instant.now().minusSeconds(3600), 5, 10);
        Order newerOrder = new Order("order2", "store1", Instant.now(), 5, 10);

        double olderScore = service.calculateOrderScore(olderOrder);
        double newerScore = service.calculateOrderScore(newerOrder);

        assertTrue(olderScore < newerScore,
            "Older order (longer wait) should have lower score");
    }

    @Test
    void calculateOrderScore_fewerSkusGetsLowerScore() {
        // Order with fewer SKUs should get lower score (faster to pick)
        Order fewSkusOrder = new Order("order1", "store1", Instant.now(), 5, 5);
        Order manySkusOrder = new Order("order2", "store1", Instant.now(), 5, 50);

        double fewSkusScore = service.calculateOrderScore(fewSkusOrder);
        double manySkusScore = service.calculateOrderScore(manySkusOrder);

        assertTrue(fewSkusScore < manySkusScore,
            "Order with fewer SKUs should have lower score");
    }

    @Test
    void calculatePickerScore_moreExperiencedPickerGetsLowerScore() {
        // More experienced picker should get lower score
        Picker experiencedPicker = new Picker("picker1", "store1", 5000, 500);
        Picker newPicker = new Picker("picker2", "store1", 100, 10);

        double experiencedScore = service.calculatePickerScore(experiencedPicker);
        double newScore = service.calculatePickerScore(newPicker);

        assertTrue(experiencedScore < newScore,
            "More experienced picker should have lower score");
    }

    @Test
    void getOrderScoreMeta_returnsCorrectMetadata() {
        Order order = new Order("order1", "store1", Instant.now().minusSeconds(300), 3, 15);

        OrderScoreMeta meta = service.getOrderScoreMeta(order);

        assertNotNull(meta);
        assertEquals(15, meta.skuScore());
        assertEquals(3, meta.initialPriority());
        assertTrue(meta.oatDelta() > 0, "OAT delta should be positive for orders past OAT (order has been waiting)");
    }

    @Test
    void getPickerScoreMeta_returnsCorrectMetadata() {
        Picker picker = new Picker("picker1", "store1", 1000, 100);

        PickerScoreMeta meta = service.getPickerScoreMeta(picker);

        assertNotNull(meta);
        assertEquals(1000, meta.skuCompletedScore());
        assertEquals(100, meta.orderCompletedScore());
    }

    @Test
    void calculateOrderScore_returnsValueBetweenZeroAndOne() {
        Order order = new Order("order1", "store1", Instant.now(), 5, 20);

        double score = service.calculateOrderScore(order);

        assertTrue(score >= 0 && score <= 1, "Score should be between 0 and 1");
    }

    @Test
    void calculatePickerScore_returnsValueBetweenZeroAndOne() {
        Picker picker = new Picker("picker1", "store1", 500, 50);

        double score = service.calculatePickerScore(picker);

        assertTrue(score >= 0 && score <= 1, "Score should be between 0 and 1");
    }
}

