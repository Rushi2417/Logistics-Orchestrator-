package com.logistics.order.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.UUID;

@Entity
@Table(name = "orders") // "order" is a reserved keyword in SQL, so we name the table "orders"
@Data // Lombok automatically generates getters, setters, toString, etc.
@NoArgsConstructor
@AllArgsConstructor
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private String customerId;
    
    private String productId;
    
    private int quantity;
    
    @Enumerated(EnumType.STRING) // Store enum as text string in Postgres
    private OrderStatus status;

    public Order(String customerId, String productId, int quantity) {
        this.customerId = customerId;
        this.productId = productId;
        this.quantity = quantity;
        this.status = OrderStatus.PENDING; // Initial state
    }
}
