package com.logistics.order.repository;

import com.logistics.order.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface OrderRepository extends JpaRepository<Order, UUID> {
    // Spring Data JPA automatically provides basic CRUD operations:
    // save(), findById(), findAll(), deleteById(), etc.
    // We don't even need to write SQL!
}
