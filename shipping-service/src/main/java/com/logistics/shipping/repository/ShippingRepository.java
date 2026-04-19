package com.logistics.shipping.repository;

import com.logistics.shipping.model.Shipping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ShippingRepository extends JpaRepository<Shipping, UUID> {
    // Basic CRUD operations available out-of-the-box
}
