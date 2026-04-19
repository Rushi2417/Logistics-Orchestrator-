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
        try {
            log.info("OrderEventConsumer received payload: {}", eventPayload);

            // ShippingEvent always has "driverId" key (even if null) due to @JsonInclude(ALWAYS)
            if (eventPayload.containsKey("driverId")) {
                ShippingEvent event = objectMapper.convertValue(eventPayload, ShippingEvent.class);
                log.info("Detected ShippingEvent for Order {}: status={}", event.getOrderId(), event.getStatus());
                processShippingEvent(event);
            }
            // InventoryReservedEvent has "message" + "status" but no "driverId"
            else if (eventPayload.containsKey("message") && eventPayload.containsKey("status")) {
                InventoryReservedEvent event = objectMapper.convertValue(eventPayload, InventoryReservedEvent.class);
                log.info("Detected InventoryReservedEvent for Order {}: status={}", event.getOrderId(), event.getStatus());
                processInventoryEvent(event);
            } else {
                log.warn("Unknown event payload, discarding: {}", eventPayload);
            }
        } catch (Exception e) {
            // Log and swallow — never throw, prevents infinite requeue loop
            log.error("Failed to process event payload, discarding: {}", eventPayload, e);
        }
    }

    private void processInventoryEvent(InventoryReservedEvent event) {
        Optional<Order> orderOpt = orderRepository.findById(UUID.fromString(event.getOrderId()));
        if (orderOpt.isPresent()) {
            Order order = orderOpt.get();
            if ("FAILED".equals(event.getStatus())) {
                order.setStatus(OrderStatus.CANCELLED);
            } else {
                order.setStatus(OrderStatus.INVENTORY_RESERVED);
            }
            orderRepository.save(order);
            log.info("Order {} updated to {}", order.getId(), order.getStatus());
            messagingTemplate.convertAndSend("/topic/orders", order);
        } else {
            log.warn("Order not found for InventoryEvent: {}", event.getOrderId());
        }
    }

    private void processShippingEvent(ShippingEvent event) {
        Optional<Order> orderOpt = orderRepository.findById(UUID.fromString(event.getOrderId()));
        if (orderOpt.isPresent()) {
            Order order = orderOpt.get();
            if ("FAILED".equals(event.getStatus())) {
                order.setStatus(OrderStatus.CANCELLED);
            } else {
                order.setStatus(OrderStatus.COMPLETED);
            }
            orderRepository.save(order);
            log.info("Order {} updated to {}", order.getId(), order.getStatus());
            messagingTemplate.convertAndSend("/topic/orders", order);
        } else {
            log.warn("Order not found for ShippingEvent: {}", event.getOrderId());
        }
    }
}
