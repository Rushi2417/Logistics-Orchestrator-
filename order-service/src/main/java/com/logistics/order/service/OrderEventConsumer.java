package com.logistics.order.service;

import com.logistics.order.event.InventoryReservedEvent;
import com.logistics.order.event.ShippingEvent;
import com.logistics.order.model.Order;
import com.logistics.order.model.OrderStatus;
import com.logistics.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderEventConsumer {

    private final OrderRepository orderRepository;
    private final org.springframework.messaging.simp.SimpMessagingTemplate messagingTemplate;

    @KafkaListener(topics = "inventory-events", groupId = "order-group")
    public void processInventoryEvent(InventoryReservedEvent event) {
        log.info("Order Service received InventoryEvent for Order {}: {}", event.getOrderId(), event.getStatus());
        
        Optional<Order> orderOpt = orderRepository.findById(UUID.fromString(event.getOrderId()));
        if (orderOpt.isPresent()) {
            Order order = orderOpt.get();
            if ("FAILED".equals(event.getStatus())) {
                log.error("SAGA TRIGGER: Inventory failed. Cancelling order {}.", order.getId());
                order.setStatus(OrderStatus.CANCELLED);
                orderRepository.save(order);
                messagingTemplate.convertAndSend("/topic/orders", order);
            } else {
                log.info("Inventory reserved. Updating order {} to INVENTORY_RESERVED.", order.getId());
                order.setStatus(OrderStatus.INVENTORY_RESERVED);
                orderRepository.save(order);
                messagingTemplate.convertAndSend("/topic/orders", order);
            }
        }
    }

    @KafkaListener(topics = "shipping-events", groupId = "order-group")
    public void processShippingEvent(ShippingEvent event) {
        log.info("Order Service received ShippingEvent for Order {}: {}", event.getOrderId(), event.getStatus());
        
        Optional<Order> orderOpt = orderRepository.findById(UUID.fromString(event.getOrderId()));
        if (orderOpt.isPresent()) {
            Order order = orderOpt.get();
            if ("FAILED".equals(event.getStatus())) {
                log.error("SAGA TRIGGER: Shipping failed. Cancelling order {}.", order.getId());
                order.setStatus(OrderStatus.CANCELLED);
                orderRepository.save(order);
                messagingTemplate.convertAndSend("/topic/orders", order);
            } else {
                log.info("Shipping assigned. Order {} is now COMPLETED!", order.getId());
                order.setStatus(OrderStatus.COMPLETED);
                orderRepository.save(order);
                messagingTemplate.convertAndSend("/topic/orders", order);
            }
        }
    }
}
