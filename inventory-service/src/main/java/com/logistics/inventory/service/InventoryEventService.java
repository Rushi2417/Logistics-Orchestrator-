package com.logistics.inventory.service;

import com.logistics.inventory.event.InventoryReservedEvent;
import com.logistics.inventory.event.OrderCreatedEvent;
import com.logistics.inventory.model.Inventory;
import com.logistics.inventory.repository.InventoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryEventService {

    private final InventoryRepository inventoryRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    private static final String INVENTORY_TOPIC = "inventory-events";

    // This listener consumes the event produced by the Order Service
    @KafkaListener(topics = "order-events", groupId = "inventory-group")
    public void processOrderAssignment(OrderCreatedEvent event) {
        log.info("Received OrderCreatedEvent for Order ID: {}", event.getOrderId());

        Optional<Inventory> inventoryOpt = inventoryRepository.findByProductId(event.getProductId());

        if (inventoryOpt.isPresent()) {
            Inventory inventory = inventoryOpt.get();
            if (inventory.getAvailableQuantity() >= event.getQuantity()) {
                // Reserve the item!
                inventory.setAvailableQuantity(inventory.getAvailableQuantity() - event.getQuantity());
                inventory.setReservedQuantity(inventory.getReservedQuantity() + event.getQuantity());
                inventoryRepository.save(inventory);

                log.info("Stock reserved. Emitting InventoryReservedEvent SUCCESS.");
                kafkaTemplate.send(INVENTORY_TOPIC, event.getOrderId(), 
                    new InventoryReservedEvent(event.getOrderId(), event.getProductId(), event.getQuantity(), "SUCCESS", "Stock reserved"));
                return;
            }
        }

        // If not present or out of stock, publish a FAILED event (First part of Saga rollback)
        log.error("Out of stock! Emitting InventoryReservedEvent FAILED.");
        kafkaTemplate.send(INVENTORY_TOPIC, event.getOrderId(), 
            new InventoryReservedEvent(event.getOrderId(), event.getProductId(), event.getQuantity(), "FAILED", "Out of stock / Invalid Product"));
    }

    // THE SAGA PATTERN: COMPENSATING TRANSACTION
    // If Shipping fails to find a driver, we must put the stock back into inventory!
    @KafkaListener(topics = "shipping-events", groupId = "inventory-group")
    public void processShippingFailure(com.logistics.inventory.event.ShippingEvent event) {
        if ("FAILED".equals(event.getStatus())) {
            log.warn("SAGA ROLLBACK: Received Shipping FAILED for Order ID: {}. Refunding stock.", event.getOrderId());
            
            Optional<Inventory> inventoryOpt = inventoryRepository.findByProductId(event.getProductId());
            if (inventoryOpt.isPresent()) {
                Inventory inventory = inventoryOpt.get();
                // Refund the reserved stock back to available stock
                inventory.setReservedQuantity(inventory.getReservedQuantity() - event.getQuantity());
                inventory.setAvailableQuantity(inventory.getAvailableQuantity() + event.getQuantity());
                inventoryRepository.save(inventory);
                
                log.info("SAGA ROLLBACK COMPLETED: Stock refunded for Product: {}", event.getProductId());
            }
        }
    }
}
