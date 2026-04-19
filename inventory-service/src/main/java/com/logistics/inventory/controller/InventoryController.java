package com.logistics.inventory.controller;

import com.logistics.inventory.model.Inventory;
import com.logistics.inventory.repository.InventoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryRepository inventoryRepository;

    @PostMapping
    public ResponseEntity<Inventory> addStock(@RequestParam String productId, @RequestParam int quantity) {
        Inventory inventory = inventoryRepository.findByProductId(productId)
            .orElse(new Inventory(productId, 0));
        
        inventory.setAvailableQuantity(inventory.getAvailableQuantity() + quantity);
        return ResponseEntity.ok(inventoryRepository.save(inventory));
    }

    @GetMapping
    public ResponseEntity<List<Inventory>> getAllInventory() {
        return ResponseEntity.ok(inventoryRepository.findAll());
    }
}
