package com.example.demo.controller;

import com.example.demo.dto.AllocationResponse;
import com.example.demo.dto.OrderRequest;
import com.example.demo.dto.PickerRequest;
import com.example.demo.dto.QueueStatusResponse;
import com.example.demo.model.Allocation;
import com.example.demo.model.Order;
import com.example.demo.model.Picker;
import com.example.demo.service.AllocationService;
import com.example.demo.service.QueueService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

/**
 * REST controller for allocation operations.
 */
@RestController
@RequestMapping("/api/v1/allocation")
public class AllocationController {

    private final QueueService queueService;
    private final AllocationService allocationService;

    public AllocationController(QueueService queueService, AllocationService allocationService) {
        this.queueService = queueService;
        this.allocationService = allocationService;
    }

    /**
     * Add an order to the queue and trigger allocation.
     * Called when: Order confirmed, Order crosses OAT
     */
    @PostMapping("/orders")
    public ResponseEntity<AllocationResponse> enqueueOrder(@Valid @RequestBody OrderRequest request) {
        Instant oat = request.oatTimestamp() != null
            ? Instant.ofEpochMilli(request.oatTimestamp())
            : Instant.now();

        Order order = new Order(
            request.orderId(),
            request.storeId(),
            oat,
            request.priorityOrder(),
            request.skuCount()
        );

        queueService.enqueueOrder(order);

        // Trigger allocation attempt
        Optional<Allocation> allocation = allocationService.triggerAllocation(request.storeId());

        return allocation
            .map(a -> ResponseEntity.ok(AllocationResponse.success(a.orderId(), a.pickerId(), a.storeId(), a.timestamp())))
            .orElse(ResponseEntity.ok(AllocationResponse.noAllocation(request.storeId())));
    }

    /**
     * Add a picker to the queue and trigger allocation.
     * Called when: Picker logs in, Picker finishes an order, Picker becomes available after role change
     */
    @PostMapping("/pickers")
    public ResponseEntity<AllocationResponse> enqueuePicker(@Valid @RequestBody PickerRequest request) {
        Picker picker = new Picker(
            request.pickerId(),
            request.storeId(),
            request.skuCompleted(),
            request.orderCompleted()
        );

        queueService.enqueuePicker(picker);

        // Trigger allocation attempt
        Optional<Allocation> allocation = allocationService.triggerAllocation(request.storeId());

        return allocation
            .map(a -> ResponseEntity.ok(AllocationResponse.success(a.orderId(), a.pickerId(), a.storeId(), a.timestamp())))
            .orElse(ResponseEntity.ok(AllocationResponse.noAllocation(request.storeId())));
    }

    /**
     * Manually trigger allocation for a store.
     */
    @PostMapping("/trigger/{storeId}")
    public ResponseEntity<AllocationResponse> triggerAllocation(@PathVariable String storeId) {
        Optional<Allocation> allocation = allocationService.triggerAllocation(storeId);

        return allocation
            .map(a -> ResponseEntity.ok(AllocationResponse.success(a.orderId(), a.pickerId(), a.storeId(), a.timestamp())))
            .orElse(ResponseEntity.ok(AllocationResponse.noAllocation(storeId)));
    }

    /**
     * Get queue status for a store.
     */
    @GetMapping("/status/{storeId}")
    public ResponseEntity<QueueStatusResponse> getQueueStatus(@PathVariable String storeId) {
        long orderQueueSize = queueService.getOrderQueueSize(storeId);
        long pickerQueueSize = queueService.getPickerQueueSize(storeId);
        boolean canAllocate = queueService.canAllocate(storeId);

        String topOrderId = queueService.peekTopOrder(storeId).orElse(null);
        String topPickerId = queueService.peekTopPicker(storeId).orElse(null);

        Map<Object, Object> topOrderScoreMeta = topOrderId != null
            ? queueService.getOrderScoreMeta(topOrderId)
            : Map.of();
        Map<Object, Object> topPickerScoreMeta = topPickerId != null
            ? queueService.getPickerScoreMeta(topPickerId)
            : Map.of();

        QueueStatusResponse response = new QueueStatusResponse(
            storeId,
            orderQueueSize,
            pickerQueueSize,
            canAllocate,
            topOrderId,
            topPickerId,
            topOrderScoreMeta,
            topPickerScoreMeta
        );

        return ResponseEntity.ok(response);
    }

    /**
     * Remove an order from the queue.
     */
    @DeleteMapping("/orders/{storeId}/{orderId}")
    public ResponseEntity<Void> dequeueOrder(@PathVariable String storeId, @PathVariable String orderId) {
        queueService.dequeueOrder(storeId, orderId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Remove a picker from the queue.
     */
    @DeleteMapping("/pickers/{storeId}/{pickerId}")
    public ResponseEntity<Void> dequeuePicker(@PathVariable String storeId, @PathVariable String pickerId) {
        queueService.dequeuePicker(storeId, pickerId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Get score metadata for an order.
     */
    @GetMapping("/orders/{orderId}/score")
    public ResponseEntity<Map<Object, Object>> getOrderScoreMeta(@PathVariable String orderId) {
        Map<Object, Object> meta = queueService.getOrderScoreMeta(orderId);
        if (meta.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(meta);
    }

    /**
     * Get score metadata for a picker.
     */
    @GetMapping("/pickers/{pickerId}/score")
    public ResponseEntity<Map<Object, Object>> getPickerScoreMeta(@PathVariable String pickerId) {
        Map<Object, Object> meta = queueService.getPickerScoreMeta(pickerId);
        if (meta.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(meta);
    }
}

