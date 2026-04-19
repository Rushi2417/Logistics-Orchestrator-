package com.logistics.shipping.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.logistics.shipping.config.RabbitMQConfig;
import com.logistics.shipping.event.InventoryReservedEvent;
import com.logistics.shipping.event.ShippingEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Random;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ShippingEventService {

    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;
    private final Random random = new Random();

    @RabbitListener(queues = RabbitMQConfig.SHIPPING_QUEUE)
    public void processInventoryEvent(Map<String, Object> eventPayload) {
        if (eventPayload.containsKey("message") && eventPayload.containsKey("status")) {
            InventoryReservedEvent event = objectMapper.convertValue(eventPayload, InventoryReservedEvent.class);
            if ("SUCCESS".equals(event.getStatus())) {
                log.info("Shipping Service allocating driver for Order: {}", event.getOrderId());

                try {
                    Thread.sleep(2000); // Simulate external API call
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }

                // 20% chance to fail to demonstrate SAGA Compensation
                if (random.nextInt(10) < 2) {
                    log.error("Failed to allocate driver! Triggering SAGA Rollback.");
                    ShippingEvent failureEvent = new ShippingEvent(event.getOrderId(), event.getProductId(), event.getQuantity(), "FAILED", null, "Driver allocation failed");
                    // Send failure to Inventory (to refund stock) and Order (to cancel order)
                    rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE, "inventory.rollback", failureEvent);
                    rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE, "order.update", failureEvent);
                } else {
                    String driverId = "DRIVER-" + UUID.randomUUID().toString().substring(0, 5);
                    log.info("Driver allocated successfully: {}", driverId);
                    ShippingEvent successEvent = new ShippingEvent(event.getOrderId(), event.getProductId(), event.getQuantity(), "SUCCESS", driverId, "Driver allocated");
                    rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE, "order.update", successEvent);
                }
            }
        }
    }
}
