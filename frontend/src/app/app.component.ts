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
  private stompClient: Client | null = null;

  ngOnInit() {
    this.connectToWebSockets();
    // Fetch initial state via REST API (in a real app)
    this.fetchOrders();
  }

  ngOnDestroy() {
    if (this.stompClient) {
      this.stompClient.deactivate();
    }
  }

  connectToWebSockets() {
    this.stompClient = new Client({
      webSocketFactory: () => new SockJS('https://order-service-jkdt.onrender.com/ws'),
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
      // Update existing order (e.g. from PENDING -> COMPLETED)
      this.orders[index] = updatedOrder;
    } else {
      // Add new order
      this.orders.unshift(updatedOrder);
    }
  }

  fetchOrders() {
    fetch('https://order-service-jkdt.onrender.com/api/orders')
      .then(res => res.json())
      .then((data: Order[]) => {
        this.orders = data;
      })
      .catch(err => console.error("Could not fetch orders. Make sure backend is running.", err));
  }

  simulateOrder() {
    const payload = {
      customerId: "user-" + Math.floor(Math.random() * 1000),
      productId: "PROD-100",
      quantity: 1
    };

    fetch('https://order-service-jkdt.onrender.com/api/orders', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload)
    }).then(res => res.json())
      .catch(err => console.error(err));
  }
}
