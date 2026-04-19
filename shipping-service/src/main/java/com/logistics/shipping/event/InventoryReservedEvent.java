package com.logistics.shipping.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InventoryReservedEvent {
    private String orderId;
    private String productId;
    private int quantity;
    private String status;
    private String message;
}
