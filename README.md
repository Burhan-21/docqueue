# DocQueue — Doctor Appointment & Real-Time Queue Management

<div align="center">

[![Java](https://img.shields.io/badge/Java-17-ED8B00?logo=openjdk&logoColor=white)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.2.5-6DB33F?logo=spring&logoColor=white)](https://spring.io/projects/spring-boot)
[![React](https://img.shields.io/badge/React-18-61DAFB?logo=react&logoColor=black)](https://react.dev/)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-15-4169E1?logo=postgresql&logoColor=white)](https://postgresql.org)
[![Redis](https://img.shields.io/badge/Redis-7-DC382D?logo=redis&logoColor=white)](https://redis.io)
[![WebSocket](https://img.shields.io/badge/WebSocket-STOMP-purple)](https://stomp.github.io/)
[![Docker](https://img.shields.io/badge/Docker-Compose-2496ED?logo=docker&logoColor=white)](https://docs.docker.com/compose/)
[![Tests](https://img.shields.io/badge/Tests-25_passing-brightgreen?logo=checkmarx)](/)

> **Production-grade SaaS for Indian clinics** — Replaces paper tokens and phone-based coordination with a live, role-aware queue management platform. Reduces patient wait uncertainty from hours to minutes.

</div>

---

## ✨ Key Features

| Feature | Details |
|---|---|
| 🔐 **JWT Auth + RBAC** | 3 roles: Patient · Doctor · Admin · 15-min access / 7-day refresh |
| 📅 **Appointment Booking** | Time-slot availability, conflict detection, token assignment |
| 📡 **Live Queue (WebSocket)** | Real-time STOMP push — doctor calls next, patients see position live |
| 📊 **Analytics Dashboard** | 7-day trends, peak hours, per-doctor throughput via Recharts |
| 📨 **Notifications** | Async email (JavaMail) + SMS (Twilio) with retry support |
| 🛡️ **Rate Limiting** | Bucket4j — 5/min auth, 10/min booking, 60/min general |
| 🗄️ **Redis Caching** | Doctor availability + queue state cached with TTL |
| 🩺 **Soft Deletes** | No hard deletes on sensitive medical entities |
| 📝 **Audit Log** | All state changes tracked with actor, timestamp, old → new value |
| 🐳 **Dockerized** | PostgreSQL + Redis + App + Nginx compose stack, ready to deploy |

---

## 🏗️ Architecture

**Modular Monolith** — 10 decoupled bounded contexts, clean migration path to microservices.

```
┌─────────────────────────────────────────┐
│         React 18 SPA (Vite + Tailwind)  │
│  Patient Portal · Doctor Portal · Admin │
└──────────────┬──────────────────────────┘
               │ REST + WebSocket (STOMP)
        ┌──────▼────────┐
        │  Nginx Proxy  │
        └──────┬────────┘
               │ :8080
┌──────────────▼─────────────────────────┐
│        Spring Boot 3.2 Monolith         │
│                                         │
│  ┌──────┐  ┌──────────┐  ┌──────────┐  │
│  │ auth │  │  doctor  │  │ patient  │  │
│  └──────┘  └──────────┘  └──────────┘  │
│  ┌─────────────┐  ┌─────────────────┐  │
│  │ appointment │  │      queue      │  │
│  └─────────────┘  └─────────────────┘  │
│  ┌──────────────┐  ┌──────────────┐    │
│  │ notification │  │  analytics   │    │
│  └──────────────┘  └──────────────┘    │
│  ┌────────┐  ┌──────┐  ┌────────────┐  │
│  │ clinic │  │ user │  │   common   │  │
│  └────────┘  └──────┘  └────────────┘  │
└────────────────────────────────────────┘
               │                │
         ┌──────────▼──┐       ┌─────▼──┐
        │ PostgreSQL │       │  Redis │
        │     15     │       │    7   │
        └────────────┘       └────────┘
```

---

## 🚀 Quick Start (Docker)

### Prerequisites
- Docker & Docker Compose installed
- Ports 80, 8080, 5433, 6379 available

### 1. Clone & configure
```bash
git clone https://github.com/your-username/docqueue.git
cd docqueue
cp .env.example .env
# Edit .env — fill in JWT_SECRET, MAIL_USER, MAIL_PASS, TWILIO_* credentials
```

### 2. Start the full stack
```bash
docker-compose up -d
```

### 3. Verify
```
API Health:  http://localhost:8080/actuator/health
Swagger UI:  http://localhost:8080/swagger-ui.html
Frontend:    http://localhost:3000
```

### Default Admin Login
```
Email:    admin@docqueue.in
Password: Admin@1234   ← change via ADMIN_PASSWORD env var in production
```

---

## ☁️ Cloud Deployment (Vercel + Render)

### Backend → Render
1. Go to [render.com](https://render.com) → **New** → **Blueprint**
2. Connect GitHub repo → Render reads `render.yaml` and provisions:
   - PostgreSQL database (free tier)
   - Redis instance (free tier)
   - Web Service (Docker, free tier)
3. Set secret env vars: `ADMIN_PASSWORD`, `FRONTEND_URL` (your Vercel URL)
4. Deploy → verify at `/actuator/health`

### Frontend → Vercel
1. Go to [vercel.com](https://vercel.com) → **Add New Project**
2. Import repo → set **Root Directory** = `frontend`
3. Add env var: `VITE_API_URL` = `https://your-backend.onrender.com/api/v1`
4. Deploy

### Link Both
Set `FRONTEND_URL` on Render = your Vercel URL → redeploy backend.

---

## 🔧 Local Development

```bash
# Start infrastructure only (PostgreSQL + Redis)
docker-compose up -d postgres redis

# Backend (Spring Boot)
./mvnw spring-boot:run

# Frontend (Vite dev server with HMR + API proxy)
cd frontend && npm install && npm run dev
```

The Vite dev server proxies `/api` and `/ws` to `http://localhost:8080` automatically.

---

## 📡 API Reference

| Module | Base URL | Auth Required |
|---|---|---|
| Auth | `POST /api/v1/auth/register` `POST /api/v1/auth/login` | Public |
| Doctors | `GET /api/v1/doctors` `GET /api/v1/doctors/{id}/slots` | Public (read) · ADMIN (write) |
| Appointments | `POST /api/v1/appointments` `GET /api/v1/appointments/my` | PATIENT |
| Queue | `GET /api/v1/queue/doctor/{id}` `POST /api/v1/queue/next` | DOCTOR |
| Analytics | `GET /api/v1/analytics/clinic/{id}/summary` | ADMIN |
| WebSocket | `/ws` (STOMP handshake) | JWT via `Authorization` header |

### WebSocket Topics
```
SUBSCRIBE /topic/queue/{doctorId}    → live queue state for a doctor's waiting room
SUBSCRIBE /topic/patient/{userId}    → personal turn notifications (SMS + push)
SEND      /app/queue/next            → doctor triggers "call next patient"
```

---

## 📋 Environment Variables

| Variable | Description | Example |
|---|---|---|
| `DB_URL` | PostgreSQL JDBC connection | `jdbc:postgresql://localhost:5432/docqueue` |
| `DB_USER` | PostgreSQL username | `docqueue` |
| `DB_PASS` | PostgreSQL password | `secret` |
| `REDIS_HOST` | Redis hostname | `localhost` |
| `JWT_SECRET` | Base64-encoded secret (≥256-bit) | `base64encodedstring` |
| `ADMIN_EMAIL` | Seeded admin email | `admin@docqueue.in` |
| `ADMIN_PASSWORD` | Seeded admin password | `Admin@1234` |
| `MAIL_HOST` | SMTP host | `smtp.gmail.com` |
| `MAIL_USER` | SMTP username | `you@gmail.com` |
| `MAIL_PASS` | SMTP app password | `xxxx xxxx xxxx xxxx` |
| `TWILIO_SID` | Twilio Account SID | `ACxxxxxxxx` |
| `TWILIO_TOKEN` | Twilio Auth Token | `xxxxxxxx` |
| `TWILIO_FROM` | Sender phone number | `+1234567890` |

---

## 🗄️ Database Schema

11 tables with full indexing strategy, managed by **Flyway** migrations:

| Table | Purpose |
|---|---|
| `clinics` | Multi-tenant clinic root entity |
| `users` | Base user entity (all roles) |
| `roles` / `user_roles` | RBAC role assignment |
| `patients` | Patient health profile |
| `doctors` | Doctor profile + consultation time + fee |
| `doctor_availability` | Weekly recurring time slots |
| `appointments` | Booking records with unique token numbers |
| `queue_entries` | Real-time queue state (WAITING → IN_PROGRESS → DONE) |
| `notifications` | Outbound email/SMS log with retry tracking |
| `audit_logs` | Immutable audit trail for all entity mutations |

---

## 🔒 Security Design

- **JWT** — Stateless access tokens (15 min) + Redis-backed refresh tokens (7 days)
- **BCrypt** — Password hashing at strength 12
- **RBAC** — Method-level `@PreAuthorize` guards on every sensitive endpoint
- **Rate Limiting** — Bucket4j in-memory token buckets per IP address
- **Soft Deletes** — Doctors and clinics are deactivated, never dropped
- **Audit Trail** — Every appointment status change logged with actor ID

---

## 📊 Tech Stack

| Layer | Technology |
|---|---|
| **Language** | Java 17 |
| **Framework** | Spring Boot 3.2.5 |
| **Security** | Spring Security + JWT (jjwt 0.12.5) |
| **Database** | PostgreSQL 15 + Spring Data JPA + Hibernate |
| **Migrations** | Flyway |
| **Cache** | Redis 7 + Spring Cache |
| **Real-Time** | WebSocket + STOMP + SockJS |
| **Rate Limiting** | Bucket4j |
| **Notifications** | Twilio SMS + JavaMail |
| **API Docs** | SpringDoc OpenAPI 3 (Swagger UI) |
| **Frontend** | React 18 + Vite + Tailwind CSS |
| **State** | TanStack Query v5 |
| **Charts** | Recharts |
| **Deployment** | Vercel (frontend) + Render (backend) + Docker |
| **CI/CD** | GitHub Actions |

---

## 🧪 Testing

```bash
# Run all 25 unit + integration tests
mvn test

# Test coverage includes:
# - AuthService (register, login, OTP verify, refresh, resend)
# - QueueService (enqueue, call-next, skip, cancel)
# - AuthController integration tests (MockMvc)
```

---

## 📦 Project Structure

```
docqueue/
├── src/main/java/com/docqueue/
│   ├── auth/           # JWT, OTP, refresh token logic
│   ├── user/           # Base User entity + repository
│   ├── clinic/         # Clinic CRUD
│   ├── doctor/         # Doctor profiles, availability slots
│   ├── patient/        # Patient profiles
│   ├── appointment/    # Booking engine + token assignment
│   ├── queue/          # Real-time queue + WebSocket broadcast
│   ├── notification/   # Async email + SMS service
│   ├── analytics/      # Dashboard metrics aggregation
│   └── common/         # JWT filter, ApiResponse, seed data
├── frontend/
│   ├── src/
│   │   ├── pages/      # auth/ · patient/ · doctor/ · admin/
│   │   ├── components/ # AppLayout, ProtectedRoute
│   │   ├── context/    # AuthContext (JWT + user state)
│   │   ├── hooks/      # useWebSocket (STOMP)
│   │   └── api/        # Axios instance + all API calls
│   └── tailwind.config.js
├── docker/             # Nginx, Grafana, Prometheus configs
├── docker-compose.yml
└── .env.example
```

---

## 📄 License

Private — All rights reserved. Contact the author for licensing inquiries.
