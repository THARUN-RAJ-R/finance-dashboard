# 💰 Finance Dashboard Backend

> 🏆 **126 Integration Tests Passed (100% Success Rate).** 
> *Fully tested across HTTP layers, Security, Concurrency, and Transactions using JUnit 5 & Testcontainers.*

A Spring Boot 3 REST API built for high-performance financial data processing, strict access control, and robust auditability.

---

## 💻 Tech Stack
*   **Java 17** | **Spring Boot 3.3.4** | **PostgreSQL 16** | **Redis 7** | **JUnit 5 / Testcontainers**

## 🚀 Core Architectural Features

*   **🛡️ Strict RBAC & Stateless Security**: Method-level security via Spring Security 6 with Redis-backed Refresh Token rotation.
*   **⚡ Smart Caching & Rate Limiting**: Distributed Bucket4j rate limiting and HTTP 304 (ETag) caching using Redis.
*   **🔐 Optimistic Locking & Soft Deletes**: `@Version` mapping to `If-Match` headers. Active partial-indexes for `deleted_at`.
*   **📜 Async Audit System**: Non-blocking `REQUIRES_NEW` transactions track every mutation transparently.
*   **💎 Idempotency & Data Filtering**: `Idempotency-Key` headers for safe retries, and dynamic JPA `Specification` queries.

---

## 🏗️ System Architecture

```mermaid
flowchart TD
    subgraph Client Layer
        C[Client App / Postman]
    end
    
    subgraph Spring Boot Application
        Filter[Security & Rate Limit Filter]
        Controllers[REST Controllers]
        Services[Business Logic & Specs]
        Audit[Async Audit Engine]
        Repos[Spring Data JPA]
    end
    
    subgraph Infrastructure
        Redis[(Redis Cache)]
        DB[(PostgreSQL 16)]
    end

    C -->|HTTPS| Filter
    Filter -->|Validates JWT & Rate Limits| Redis
    Filter --> Controllers
    Controllers --> Services
    Services -->|ETag checks| Redis
    Services -.->|Fires Async Event| Audit
    Services --> Repos
    Repos -->|HikariCP| DB
    Audit -->|New Transaction| DB
```

## 🗄️ Database Schema 

```mermaid
erDiagram
    users ||--o{ user_roles : "assigned"
    roles ||--o{ user_roles : "grants"
    users ||--o{ financial_records : "creates"
    categories ||--o{ financial_records : "groups"
    users ||--o{ audit_log : "triggers"

    users {
        uuid id PK
        string email
        string password_hash
        string status
    }
    
    roles {
        smallint id PK
        string name
    }

    user_roles {
        uuid user_id FK
        smallint role_id FK
    }

    categories {
        uuid id PK
        string name
    }

    financial_records {
        uuid id PK
        date record_date
        string type
        decimal amount
        uuid category_id FK
        uuid created_by FK
        timestamptz deleted_at
        integer version
    }

    audit_log {
        uuid id PK
        uuid actor_user_id FK
        string action
        string entity_type
        uuid entity_id
        jsonb metadata
    }
```

---

## 📋 API Documentation & Setup

**Interactive Explorer:** [https://tharun-raj-r.github.io/finance-dashboard/](https://tharun-raj-r.github.io/finance-dashboard/)

### 🔑 Test Credentials (RBAC)
*   **Admin**: `admin@finance.com` / `password` *(Full access)*
*   **Analyst**: `analyst@finance.com` / `password` *(View records & trends)*
*   **Viewer**: `viewer@finance.com` / `password` *(Read-only dashboard access)*

### 🏁 Quick Start
```bash
docker-compose up -d
mvn spring-boot:run
```

*Note: For the full list of endpoint routes, request schemas, and parameter filters, please refer directly to the Live API Explorer link above.*
