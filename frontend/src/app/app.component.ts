import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterOutlet } from '@angular/router';
import { Client } from '@stomp/stompjs';
import * as SockJSImport from 'sockjs-client';
const SockJS = (SockJSImport as any).default || SockJSImport;

// Polyfill for sockjs-client which relies on 'global'
(window as any).global = window;

export interface Order {
  id: string;
  productId: string;
  quantity: number;
  status: string;
  customerId: string;
}

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [CommonModule, RouterOutlet],
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.scss']
})
export class AppComponent implements OnInit, OnDestroy {
  title = 'frontend';
  orders: Order[] = [];
  isLoading = false;
  private stompClient: Client | null = null;
  private readonly ORDER_SERVICE = 'https://order-service-jkdt.onrender.com';

  ngOnInit() {
    this.connectToWebSockets();
    this.fetchOrders();
  }

  ngOnDestroy() {
    if (this.stompClient) {
      this.stompClient.deactivate();
    }
  }

  connectToWebSockets() {
    this.stompClient = new Client({
      webSocketFactory: () => new SockJS(`${this.ORDER_SERVICE}/ws`),
      debug: function (str) {
        console.log(str);
      },
      reconnectDelay: 5000,
      heartbeatIncoming: 4000,
      heartbeatOutgoing: 4000,
    });

    this.stompClient.onConnect = (frame) => {
      console.log('Connected: ' + frame);
      this.stompClient?.subscribe('/topic/orders', (message) => {
        if (message.body) {
          const updatedOrder: Order = JSON.parse(message.body);
          this.updateOrAddOrder(updatedOrder);
        }
      });
    };

    this.stompClient.activate();
  }

  updateOrAddOrder(updatedOrder: Order) {
    const index = this.orders.findIndex(o => o.id === updatedOrder.id);
    if (index !== -1) {
      this.orders[index] = { ...updatedOrder };
      // Trigger Angular change detection on the array
      this.orders = [...this.orders];
    } else {
      this.orders = [updatedOrder, ...this.orders];
    }
  }

  fetchOrders() {
    fetch(`${this.ORDER_SERVICE}/api/orders`)
      .then(res => {
        if (!res.ok) throw new Error(`HTTP ${res.status}`);
        return res.json();
      })
      .then((data: any) => {
        // Guard: ensure we received an array
        if (Array.isArray(data)) {
          this.orders = data;
        } else {
          console.warn('Unexpected response format from /api/orders:', data);
        }
      })
      .catch(err => {
        console.error('Could not fetch orders, retrying in 5s...', err);
        // Retry after 5 seconds if the backend is still warming up
        setTimeout(() => this.fetchOrders(), 5000);
      });
  }

  simulateOrder() {
    if (this.isLoading) return;
    this.isLoading = true;

    const payload = {
      customerId: 'user-' + Math.floor(Math.random() * 1000),
      productId: 'PROD-100',
      quantity: 1
    };

    fetch(`${this.ORDER_SERVICE}/api/orders`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload)
    })
      .then(res => {
        if (!res.ok) throw new Error(`HTTP ${res.status}`);
        return res.json();
      })
      .then((newOrder: Order) => {
        // Optimistically add the new order to the top immediately
        this.updateOrAddOrder(newOrder);
      })
      .catch(err => console.error('Failed to create order:', err))
      .finally(() => { this.isLoading = false; });
  }
}
