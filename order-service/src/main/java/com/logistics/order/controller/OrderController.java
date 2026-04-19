package com.logistics.order.controller;

import com.logistics.order.model.Order;
import com.logistics.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    // Spring will automatically inject the repository via constructor (thanks to @RequiredArgsConstructor)
    private final OrderRepository orderRepository;
    private final com.logistics.order.service.OrderEventProducer eventProducer;

    @PostMapping
    public ResponseEntity<Order> createOrder(@RequestBody OrderRequest request) {
        // 1. Create a new Order entity
        Order newOrder = new Order(request.getCustomerId(), request.getProductId(), request.getQuantity());
        
        // 2. Save it to the database
        Order savedOrder = orderRepository.save(newOrder);

        // 3. Publish an event to Kafka allowing other services to react
        com.logistics.order.event.OrderCreatedEvent event = new com.logistics.order.event.OrderCreatedEvent(
            savedOrder.getId().toString(),
            savedOrder.getProductId(),
            savedOrder.getQuantity(),
            savedOrder.getCustomerId()
        );
        eventProducer.publishOrderCreatedEvent(event);

        // 4. Return 201 Created and the saved entity
        return ResponseEntity.status(HttpStatus.CREATED).body(savedOrder);
    }

    @GetMapping
    public ResponseEntity<List<Order>> getAllOrders() {
        return ResponseEntity.ok(orderRepository.findAll());
    }
}
