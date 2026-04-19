# 🚀 Distributed Logistics Orchestrator

A production-grade, event-driven microservices architecture demonstrating the **Saga Choreography Pattern** for distributed transaction management. Built with Spring Boot, RabbitMQ, and Angular — deployed on **Render** (backend) and **Vercel** (frontend).

[![Live Demo](https://img.shields.io/badge/Live%20Demo-logistics--orchestrator.vercel.app-blue?style=for-the-badge)](https://logistics-orchestrator.vercel.app)
[![Backend](https://img.shields.io/badge/Backend-Render-46E3B7?style=for-the-badge)](https://order-service-jkdt.onrender.com/api/orders)

---

## 📐 Architecture

```
┌─────────────┐     REST POST      ┌──────────────────┐
│   Angular   │ ────────────────▶  │   order-service  │
│  Frontend   │ ◀──────── WS ───── │   (Port 8080)    │
│  (Vercel)   │                    └────────┬─────────┘
└─────────────┘                             │ order.created
                                            ▼
                                   ┌──────────────────┐
                                   │ inventory-service │
                                   │   (Port 8082)    │
                                   └────────┬─────────┘
                             ┌──────────────┴──────────────┐
                        inventory.reserved           inventory.failed
                             │                             │
                             ▼                             ▼
                    ┌──────────────────┐         SAGA ROLLBACK:
                    │ shipping-service │         Order → CANCELLED
                    │   (Port 8083)   │
                    └────────┬────────┘
                   ┌─────────┴─────────┐
              shipping.success    shipping.failed
                   │                   │
                   ▼                   ▼
           Order → COMPLETED   SAGA ROLLBACK:
                               Stock refunded +
                               Order → CANCELLED
```

### Saga State Machine

```
PENDING → INVENTORY_RESERVED → COMPLETED
   │               │
   │           (20% chance)
   │               ↓
   └──────── CANCELLED (by Saga Rollback)
```

---

## 🛠 Tech Stack

| Layer        | Technology                          |
|--------------|-------------------------------------|
| Backend      | Java 17, Spring Boot 4, Spring AMQP |
| Database     | PostgreSQL (Supabase)               |
| Message Bus  | RabbitMQ (CloudAMQP)               |
| Frontend     | Angular 17, STOMP.js, SockJS        |
| Containers   | Docker, Docker Compose              |
| Deployment   | Render (backend), Vercel (frontend) |

---

## 🌐 Live Endpoints

| Service           | URL                                                            |
|-------------------|----------------------------------------------------------------|
| **Frontend**      | https://logistics-orchestrator.vercel.app                      |
| **Order Service** | https://order-service-jkdt.onrender.com/api/orders             |
| **Inventory**     | https://inventory-service-cnr4.onrender.com                    |
| **Shipping**      | https://shipping-service-ewzs.onrender.com                     |

> ⚠️ Render free-tier services spin down after 15 min of inactivity. The first request may take **30–60 seconds** to cold-start.

---

## 🧪 How to Test

### Option 1 — Live Demo (Easiest)

1. Open **https://logistics-orchestrator.vercel.app** in your browser
2. Wait ~5 seconds for the existing orders to load in the table
3. Click **"Simulate New Order"**
4. Watch the row appear instantly as **`PENDING`**
5. Within ~3 seconds it transitions to **`INVENTORY_RESERVED`**
6. Within ~5 seconds it resolves to either:
   - ✅ **`COMPLETED`** — Driver allocated successfully (80% chance)
   - ❌ **`CANCELLED`** — Saga rollback triggered, stock refunded (20% chance)

> All status transitions are real-time via WebSocket — **no page refresh needed.**

---

### Option 2 — REST API with curl

**Create an order:**
```bash
curl -X POST https://order-service-jkdt.onrender.com/api/orders \
  -H "Content-Type: application/json" \
  -d '{"customerId":"test-user-1","productId":"PROD-100","quantity":1}'
```

**List all orders:**
```bash
curl https://order-service-jkdt.onrender.com/api/orders
```

**Expected response (new order):**
```json
{
  "id": "a1b2c3d4-...",
  "customerId": "test-user-1",
  "productId": "PROD-100",
  "quantity": 1,
  "status": "PENDING"
}
```

---

### Option 3 — Run Locally with Docker Compose

#### Prerequisites
- Docker Desktop installed and running
- Git

#### Steps

```bash
# 1. Clone the repository
git clone https://github.com/Rushi2417/Logistics-Orchestrator-.git
cd Logistics-Orchestrator-

# 2. Start all services (RabbitMQ, 3 microservices, PostgreSQL)
docker-compose up --build

# 3. Open the frontend
cd frontend
npm install
ng serve
# Visit http://localhost:4200
```

Services will be available at:
| Service           | Local URL                       |
|-------------------|---------------------------------|
| Order Service     | http://localhost:8080/api/orders |
| Inventory Service | http://localhost:8082            |
| Shipping Service  | http://localhost:8083            |
| RabbitMQ Console  | http://localhost:15672 (guest/guest) |

---

## 📦 Project Structure

```
├── order-service/          # REST API + Saga coordinator + WebSocket emitter
│   ├── src/main/java/com/logistics/order/
│   │   ├── controller/     # OrderController (POST /api/orders, GET /api/orders)
│   │   ├── service/        # OrderEventConsumer (RabbitMQ listener)
│   │   ├── model/          # Order entity + OrderStatus enum
│   │   ├── config/         # RabbitMQConfig, WebSocketConfig
│   │   └── event/          # OrderCreatedEvent, InventoryReservedEvent, ShippingEvent
│   └── Dockerfile
│
├── inventory-service/      # Stock management + reservation logic
│   ├── src/main/java/com/logistics/inventory/
│   │   ├── service/        # InventoryEventService (listener + publisher)
│   │   ├── model/          # Inventory entity
│   │   └── config/         # RabbitMQConfig
│   └── Dockerfile
│
├── shipping-service/       # Driver allocation + Saga compensation trigger
│   ├── src/main/java/com/logistics/shipping/
│   │   ├── service/        # ShippingEventService (20% failure simulation)
│   │   └── config/         # RabbitMQConfig
│   └── Dockerfile
│
├── frontend/               # Angular 17 real-time dashboard
│   └── src/app/
│       ├── app.component.ts     # WebSocket + REST logic
│       ├── app.component.html   # Orders table
│       └── app.component.scss   # Styling
│
├── docker-compose.yml      # Local orchestration
├── render.yaml             # Render deployment config
└── init-dbs.sql            # Initial DB schema
```

---

## 🔄 Saga Flow Explained

### Happy Path (80% of orders)
```
1. POST /api/orders  →  Order saved as PENDING
2. order-service     →  Publishes OrderCreatedEvent to inventory.queue
3. inventory-service →  Reserves stock, publishes InventoryReservedEvent (SUCCESS)
                         to both shipping.queue and order.queue
4. order-service     →  Updates Order → INVENTORY_RESERVED, pushes via WebSocket
5. shipping-service  →  Allocates driver, publishes ShippingEvent (SUCCESS)
                         to order.queue
6. order-service     →  Updates Order → COMPLETED, pushes via WebSocket
```

### Saga Rollback (20% of orders — simulated failure)
```
1–4. Same as above
5. shipping-service  →  Fails to allocate driver (random), publishes ShippingEvent (FAILED)
                         to inventory.queue (rollback stock) + order.queue
6. inventory-service →  Restores reserved stock back to available
7. order-service     →  Updates Order → CANCELLED, pushes via WebSocket
```

---

## ⚙️ Environment Variables (Render)

Set these in each Render service's **Environment** tab:

### order-service / inventory-service / shipping-service
| Variable             | Description                              |
|----------------------|------------------------------------------|
| `SPRING_DATASOURCE_URL` | Supabase JDBC connection string (IPv4 pooler port 6543) |
| `SPRING_DATASOURCE_USERNAME` | Supabase DB username              |
| `SPRING_DATASOURCE_PASSWORD` | Supabase DB password              |
| `SPRING_RABBITMQ_ADDRESSES` | CloudAMQP AMQPS connection string  |
| `PORT`               | Auto-set by Render (8080 / 8082 / 8083) |

---

## 🏗 Deployment

### Backend (Render)
Each microservice has its own `Dockerfile`. Render auto-deploys on every push to `main`.

```bash
# Push to trigger auto-deploy on Render
git push origin main
```

### Frontend (Vercel)
Vercel auto-deploys from the `/frontend` directory.

> The `vercel.json` at `/frontend/vercel.json` sets `outputDirectory` to `dist/frontend/browser` and rewrites all routes to `index.html` for Angular routing.

---

## 🐛 Known Limitations (Free Tier)

| Issue | Cause | Workaround |
|-------|-------|------------|
| 30–60s cold start | Render free-tier spins down idle services | Click button, wait, refresh |
| Occasional 500 on first load | Service still booting | Frontend auto-retries every 5s |
| Max 9 DB connections | Supabase free pool limit | HikariCP capped at 3/service |

---

## 👤 Author

**Rushikesh Bhosale**  
GitHub: [@Rushi2417](https://github.com/Rushi2417)

---

## 📄 License

This project is open source and available under the [MIT License](LICENSE).
