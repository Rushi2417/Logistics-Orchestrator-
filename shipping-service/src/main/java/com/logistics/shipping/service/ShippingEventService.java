package com.logistics.shipping.service;

import com.logistics.shipping.event.InventoryReservedEvent;
import com.logistics.shipping.event.ShippingEvent;
import com.logistics.shipping.model.Shipping;
import com.logistics.shipping.model.ShippingStatus;
import com.logistics.shipping.repository.ShippingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ShippingEventService {

    private final ShippingRepository shippingRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    private static final String SHIPPING_TOPIC = "shipping-events";

    // Listen to the Inventory Service (Are there items in stock to ship?)
    @KafkaListener(topics = "inventory-events", groupId = "shipping-group")
    public void processInventoryEvent(InventoryReservedEvent event) {
        log.info("Received InventoryReservedEvent for Order ID: {} with state: {}", event.getOrderId(), event.getStatus());

        if ("FAILED".equals(event.getStatus())) {
            // Inventory failed, so shipping does nothing. The Order service will handle cancellation.
            return;
        }

        // Simulating Driver Assignment (E.g., external API call here)
        // Let's pretend 10% of the time there are no drivers available (this triggers Saga rollback!)
        boolean driversAvailable = Math.random() > 0.1;

        if (driversAvailable) {
            String driverId = "DRIVER-" + UUID.randomUUID().toString().substring(0, 5);
            
            Shipping shipping = new Shipping(event.getOrderId());
            shipping.setDriverId(driverId);
            shipping.setStatus(ShippingStatus.ASSIGNED);
            shippingRepository.save(shipping);

            log.info("Driver assigned successfully! Emitting ShippingEvent ASSIGNED.");
            kafkaTemplate.send(SHIPPING_TOPIC, event.getOrderId(), 
                new ShippingEvent(event.getOrderId(), event.getProductId(), event.getQuantity(), "ASSIGNED", driverId, "Driver assigned and en route"));
        } else {
            // CRITICAL: We could not assign a driver!
            // We must emit a FAILED event so Inventory can put the stock back (Compensating Transaction)
            log.error("No drivers available! Emitting ShippingEvent FAILED.");
            kafkaTemplate.send(SHIPPING_TOPIC, event.getOrderId(), 
                new ShippingEvent(event.getOrderId(), event.getProductId(), event.getQuantity(), "FAILED", null, "No drivers available"));
        }
    }
}
