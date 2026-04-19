package com.logistics.order.model;

public enum OrderStatus {
    PENDING,            // Order received, waiting for inventory to confirm
    INVENTORY_RESERVED, // Inventory has stock, waiting on shipping assignment
    COMPLETED,          // Shipping assigned, everything is good!
    CANCELLED           // Something failed (e.g., no drivers), rolled back via Saga
}
