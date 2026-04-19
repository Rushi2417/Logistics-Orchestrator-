package com.logistics.shipping.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.UUID;

@Entity
@Table(name = "shipping_records")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Shipping {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // References the Order ID from the Order Service
    private String orderId;
    
    private String driverId;
    
    @Enumerated(EnumType.STRING)
    private ShippingStatus status;

    public Shipping(String orderId) {
        this.orderId = orderId;
        this.status = ShippingStatus.PENDING;
    }
}
