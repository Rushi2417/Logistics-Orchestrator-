package com.logistics.order.controller;

import lombok.Data;

@Data
public class OrderRequest {
    private String customerId;
    private String productId;
    private int quantity;
}
