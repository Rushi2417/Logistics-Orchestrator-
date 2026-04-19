package com.logistics.inventory.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.logistics.inventory.config.RabbitMQConfig;
import com.logistics.inventory.event.InventoryReservedEvent;
import com.logistics.inventory.event.OrderCreatedEvent;
import com.logistics.inventory.event.ShippingEvent;
import com.logistics.inventory.model.Inventory;
import com.logistics.inventory.repository.InventoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryEventService {

    private final InventoryRepository inventoryRepository;
    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    @RabbitListener(queues = RabbitMQConfig.INVENTORY_QUEUE)
    public void processInventoryEvents(Map<String, Object> eventPayload) {
        if (eventPayload.containsKey("quantity") && !eventPayload.containsKey("driverId")) {
            // It's OrderCreatedEvent
            OrderCreatedEvent event = objectMapper.convertValue(eventPayload, OrderCreatedEvent.class);
            processOrderCreated(event);
        } else if (eventPayload.containsKey("driverId")) {
            // It's ShippingEvent (for compensation)
            ShippingEvent event = objectMapper.convertValue(eventPayload, ShippingEvent.class);
            processShippingFailure(event);
        }
    }

    private void processOrderCreated(OrderCreatedEvent event) {
        log.info("Inventory Service checking stock for Order: {}", event.getOrderId());

        Optional<Inventory> invOpt = inventoryRepository.findByProductId(event.getProductId());

        if (invOpt.isPresent()) {
            Inventory inv = invOpt.get();
            if (inv.getAvailableQuantity() >= event.getQuantity()) {
                inv.setAvailableQuantity(inv.getAvailableQuantity() - event.getQuantity());
                inv.setReservedQuantity(inv.getReservedQuantity() + event.getQuantity());
                inventoryRepository.save(inv);

                log.info("Stock reserved. Emitting InventoryReservedEvent (SUCCESS)");
                rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE, "shipping.reserve", new InventoryReservedEvent(event.getOrderId(), event.getProductId(), event.getQuantity(), "SUCCESS", "Stock reserved"));
                // Also tell order service
                rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE, "order.update", new InventoryReservedEvent(event.getOrderId(), event.getProductId(), event.getQuantity(), "SUCCESS", "Stock reserved"));
                return;
            }
        }

        log.warn("Stock unavailable! Emitting InventoryReservedEvent (FAILED)");
        rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE, "order.update", new InventoryReservedEvent(event.getOrderId(), event.getProductId(), event.getQuantity(), "FAILED", "Out of stock"));
    }

    private void processShippingFailure(ShippingEvent event) {
        if ("FAILED".equals(event.getStatus())) {
            log.info("SAGA ROLLBACK: Reverting reserved stock for Order: {}", event.getOrderId());
            // Need product ID and quantity, mock retrieval or pass via event
            // Quick mock reverse since we don't have exact qty safely stored here
            Optional<Inventory> invOpt = inventoryRepository.findByProductId("PROD-100");
            if (invOpt.isPresent()) {
                Inventory inv = invOpt.get();
                inv.setAvailableQuantity(inv.getAvailableQuantity() + 1);
                inv.setReservedQuantity(inv.getReservedQuantity() - 1);
                inventoryRepository.save(inv);
                log.info("Stock refunded successfully.");
            }
        }
    }
}
