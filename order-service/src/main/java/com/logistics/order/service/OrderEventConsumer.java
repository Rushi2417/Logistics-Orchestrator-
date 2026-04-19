package com.logistics.order.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.logistics.order.config.RabbitMQConfig;
import com.logistics.order.event.InventoryReservedEvent;
import com.logistics.order.event.ShippingEvent;
import com.logistics.order.model.Order;
import com.logistics.order.model.OrderStatus;
import com.logistics.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderEventConsumer {

    private final OrderRepository orderRepository;
    private final org.springframework.messaging.simp.SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;

    @RabbitListener(queues = RabbitMQConfig.ORDER_QUEUE)
    public void processOrderEvents(Map<String, Object> eventPayload) {
        // We use Map to bypass Jackson __TypeId__ restrictions across microservice packages.
        if (eventPayload.containsKey("reservedQuantity")) {
            // It's an InventoryReservedEvent
            InventoryReservedEvent event = objectMapper.convertValue(eventPayload, InventoryReservedEvent.class);
            processInventoryEvent(event);
        } else if (eventPayload.containsKey("driverId") || eventPayload.containsKey("status")) {
            // Check status presence for ShippingEvent
            ShippingEvent event = objectMapper.convertValue(eventPayload, ShippingEvent.class);
            processShippingEvent(event);
        }
    }

    private void processInventoryEvent(InventoryReservedEvent event) {
        log.info("Order Service received InventoryEvent for Order {}: {}", event.getOrderId(), event.getStatus());
        Optional<Order> orderOpt = orderRepository.findById(UUID.fromString(event.getOrderId()));
        if (orderOpt.isPresent()) {
            Order order = orderOpt.get();
            if ("FAILED".equals(event.getStatus())) {
                order.setStatus(OrderStatus.CANCELLED);
                orderRepository.save(order);
                messagingTemplate.convertAndSend("/topic/orders", order);
            } else {
                order.setStatus(OrderStatus.INVENTORY_RESERVED);
                orderRepository.save(order);
                messagingTemplate.convertAndSend("/topic/orders", order);
            }
        }
    }

    private void processShippingEvent(ShippingEvent event) {
        log.info("Order Service received ShippingEvent for Order {}: {}", event.getOrderId(), event.getStatus());
        Optional<Order> orderOpt = orderRepository.findById(UUID.fromString(event.getOrderId()));
        if (orderOpt.isPresent()) {
            Order order = orderOpt.get();
            if ("FAILED".equals(event.getStatus())) {
                order.setStatus(OrderStatus.CANCELLED);
                orderRepository.save(order);
                messagingTemplate.convertAndSend("/topic/orders", order);
            } else {
                order.setStatus(OrderStatus.COMPLETED);
                orderRepository.save(order);
                messagingTemplate.convertAndSend("/topic/orders", order);
            }
        }
    }
}
