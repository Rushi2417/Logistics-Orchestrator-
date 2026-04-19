package com.logistics.inventory.repository;

import com.logistics.inventory.model.Inventory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface InventoryRepository extends JpaRepository<Inventory, UUID> {
    Optional<Inventory> findByProductId(String productId);
}
