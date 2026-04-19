package com.logistics.inventory.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ShippingEvent {
    private String orderId;
    private String productId;
    private int quantity;
    private String status; // "ASSIGNED" or "FAILED"
    private String driverId;
    private String message;
}
