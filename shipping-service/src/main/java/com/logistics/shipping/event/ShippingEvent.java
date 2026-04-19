package com.logistics.shipping.event;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.ALWAYS)
public class ShippingEvent {
    private String orderId;
    private String productId;
    private int quantity;
    private String status; // "ASSIGNED" or "FAILED"
    private String driverId;
    private String message;
}
