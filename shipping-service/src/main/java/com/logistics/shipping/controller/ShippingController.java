package com.logistics.shipping.controller;

import com.logistics.shipping.model.Shipping;
import com.logistics.shipping.repository.ShippingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/shipping")
@RequiredArgsConstructor
public class ShippingController {

    private final ShippingRepository shippingRepository;

    @GetMapping
    public ResponseEntity<List<Shipping>> getAllShipments() {
        return ResponseEntity.ok(shippingRepository.findAll());
    }

    // In Phase 2, shipping records will ideally be created automatically via Kafka events, 
    // but here is a manual endpoint for testing purposes.
    @PostMapping
    public ResponseEntity<Shipping> testManualShipment(@RequestParam String orderId) {
        Shipping shipping = new Shipping(orderId);
        return ResponseEntity.ok(shippingRepository.save(shipping));
    }
}
