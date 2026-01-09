package com.example.demo.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "allocation")
public class AllocationProperties {

    private Weights weights = new Weights();
    private int scoreMetaTtlHours = 24;

    public Weights getWeights() {
        return weights;
    }

    public void setWeights(Weights weights) {
        this.weights = weights;
    }

    public int getScoreMetaTtlHours() {
        return scoreMetaTtlHours;
    }

    public void setScoreMetaTtlHours(int scoreMetaTtlHours) {
        this.scoreMetaTtlHours = scoreMetaTtlHours;
    }

    public static class Weights {
        private OrderWeights order = new OrderWeights();
        private PickerWeights picker = new PickerWeights();

        public OrderWeights getOrder() {
            return order;
        }

        public void setOrder(OrderWeights order) {
            this.order = order;
        }

        public PickerWeights getPicker() {
            return picker;
        }

        public void setPicker(PickerWeights picker) {
            this.picker = picker;
        }
    }

    public static class OrderWeights {
        private double oatDelta = 0.4;
        private double priority = 0.35;
        private double skuCount = 0.25;

        public double getOatDelta() {
            return oatDelta;
        }

        public void setOatDelta(double oatDelta) {
            this.oatDelta = oatDelta;
        }

        public double getPriority() {
            return priority;
        }

        public void setPriority(double priority) {
            this.priority = priority;
        }

        public double getSkuCount() {
            return skuCount;
        }

        public void setSkuCount(double skuCount) {
            this.skuCount = skuCount;
        }
    }

    public static class PickerWeights {
        private double skuCompleted = 0.5;
        private double orderCompleted = 0.5;

        public double getSkuCompleted() {
            return skuCompleted;
        }

        public void setSkuCompleted(double skuCompleted) {
            this.skuCompleted = skuCompleted;
        }

        public double getOrderCompleted() {
            return orderCompleted;
        }

        public void setOrderCompleted(double orderCompleted) {
            this.orderCompleted = orderCompleted;
        }
    }
}

