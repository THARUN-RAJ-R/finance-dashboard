# 💰 Finance Dashboard Backend

> 🏆 **126 Integration Tests Passed (100% Success Rate).** 
> *Fully tested across HTTP layers, Security, Concurrency, and Transactions using JUnit 5 & Testcontainers.*

A Spring Boot 3 REST API built for high-performance financial data processing, strict access control, and robust auditability.

---

## 💻 Tech Stack
*   **Java 17** | **Spring Boot 3.3.4** | **PostgreSQL 16** | **Redis 7** | **JUnit 5 / Testcontainers**

## 🚀 Core Features

This codebase focuses on solid backend engineering practices, security, and data integrity:

*   **🛡️ Strict RBAC**: Method-level security via Spring Security 6 with Redis-backed Refresh Token rotation.
*   **⚡ Smart Caching & Rate Limiting**: Distributed Bucket4j rate limiting and HTTP 304 (ETag) caching using Redis.
*   **🔐 Optimistic Locking & Soft Deletes**: `@Version` mapping to `If-Match` headers. Active partial-indexes for `deleted_at`.
*   **📜 Async Audit System**: Non-blocking `REQUIRES_NEW` transactions track every mutation transparently.
*   **💎 Idempotency & Filters**: `Idempotency-Key` headers for safe retries, and dynamic JPA `Specification` queries for advanced filtering.

---

## 🛠️ Engineering Decisions & Trade-offs

Consistent with assessing technical reasoning, here are the core trade-offs made:

**1. Data Precision (BigDecimal)**
*   **Decision**: Used `BigDecimal(19,2)` for all monetary values instead of `double`/`float`.
*   **Reasoning**: Prevents floating-point math rounding errors (e.g., `0.1 + 0.2 = 0.30000000000000004`).
*   **Trade-off**: Higher memory overhead and slightly slower calculations, but guarantees 100% currency accuracy.

**2. Stateless Auth + Redis Refresh Tokens**
*   **Decision**: JWT for fast access; UUIDs stored in Redis for refresh tokens.
*   **Reasoning**: Keeping refresh tokens in Redis allows for **instant revocation** (e.g., on logout or suspicious activity) without hitting the primary database.

**3. Soft Delete Strategy**
*   **Decision**: Financial records are never physically deleted; they are marked with a `deleted_at` timestamp.
*   **Reasoning**: Preserves a full "paper trail" for audit and forensic purposes.
*   **Trade-off**: The database grows larger over time. **Mitigation**: Implemented **Partial Indexes** (`WHERE deleted_at IS NULL`) so queries on active records remain lightning-fast.

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

---

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
        varchar email
        varchar password_hash
        varchar status
        timestamptz created_at
        timestamptz updated_at
    }
    
    roles {
        smallint id PK
        varchar name
    }

    user_roles {
        uuid user_id PK,FK
        smallint role_id PK,FK
    }

    categories {
        uuid id PK
        varchar name
        timestamptz created_at
        timestamptz deleted_at
    }

    financial_records {
        uuid id PK
        date record_date
        varchar type
        uuid category_id FK
        numeric amount
        varchar currency
        text notes
        uuid created_by FK
        timestamptz created_at
        timestamptz updated_at
        timestamptz deleted_at
        bigint version
    }

    audit_log {
        uuid id PK
        uuid actor_user_id FK
        varchar action
        varchar entity_type
        uuid entity_id
        jsonb metadata
        timestamptz created_at
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
