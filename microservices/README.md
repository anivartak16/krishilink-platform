# KrishiLink Microservices Architecture (Spring Cloud & EazyBytes Pattern)

This folder contains the complete, production-grade microservices implementation of the KrishiLink platform, designed following the **EazyBytes (Madan Reddy) Spring Cloud Microservices** architectural patterns.

---

## 🏛️ Architecture Overview

```mermaid
flowchart TD
    ReactClient["React Client (Vite :5173)"] -->|http://localhost:5000/api/**| Gateway["Spring Cloud Gateway (:5000)"]
    
    subgraph Service Discovery & Config
        Eureka["Netflix Eureka Server (:8070)"]
        Config["Spring Cloud Config Server (:8071)"]
    end
    
    Gateway -->|Discovery Routing| Eureka
    Config -.->|Externalized Config| Gateway
    Config -.->|Externalized Config| UserService
    Config -.->|Externalized Config| LotService
    Config -.->|Externalized Config| DemandService
    Config -.->|Externalized Config| TradeService
    Config -.->|Externalized Config| MarketService

    subgraph Core Domain Microservices
        UserService["user-service (:8081)\nAuth, Profiles, e-KYC, FPO"]
        LotService["lot-service (:8082)\nCrop Lots, KVK Quality Testing"]
        DemandService["demand-service (:8083)\nBuyer Demands, Matching Engine"]
        TradeService["trade-service (:8084)\nContracts, Escrow Payments, Disputes"]
        MarketService["market-service (:8085)\nMandi Prices, Warehouses, Logistics, Stats"]
    end

    Gateway --> UserService
    Gateway --> LotService
    Gateway --> DemandService
    Gateway --> TradeService
    Gateway --> MarketService

    DemandService -->|OpenFeign| LotService
    DemandService -->|OpenFeign| UserService
    TradeService -->|OpenFeign| UserService
    TradeService -->|OpenFeign| LotService
    MarketService -->|OpenFeign| UserService

    subgraph Isolated Databases (MySQL Database-Per-Service)
        DBUser[(krishilink_user)]
        DBLot[(krishilink_lot)]
        DBDemand[(krishilink_demand)]
        DBTrade[(krishilink_trade)]
        DBMarket[(krishilink_market)]
    end

    UserService --> DBUser
    LotService --> DBLot
    DemandService --> DBDemand
    TradeService --> DBTrade
    MarketService --> DBMarket
```

---

## 📦 Service Breakdown & Ports

| Service | Port | Description & Responsibilities | Database |
| :--- | :--- | :--- | :--- |
| **`eureka-server`** | `8070` | Netflix Eureka Service Discovery & Heartbeat Registry (`http://localhost:8070`) | N/A |
| **`config-server`** | `8071` | Centralized Configuration Server (`http://localhost:8071`) | N/A |
| **`gateway-server`** | `5000` | Spring Cloud Gateway with reactive routing & Global CORS | N/A |
| **`user-service`** | `8081` | JWT Authentication, Aadhaar e-KYC (Verhoeff), User profiles, FPO memberships | `krishilink_user` |
| **`lot-service`** | `8082` | Crop lot listings, KVK inspector quality grades, test reports, market status | `krishilink_lot` |
| **`demand-service`** | `8083` | Buyer demand creation, explainable multi-factor lot matching engine | `krishilink_demand` |
| **`trade-service`** | `8084` | Offer negotiation, contracts, simulated escrow payments, trade disputes | `krishilink_trade` |
| **`market-service`** | `8085` | Mandi price tickers, AI price prediction, warehouse & logistics bookings, analytics | `krishilink_market` |

---

## 🚀 Running the Microservices

### Option 1: Docker Compose (Recommended)
From the `microservices` directory:
```bash
docker compose up --build
```
This automatically boots:
1. MySQL container with healthy readiness checks
2. Eureka Server (`:8070`)
3. Config Server (`:8071`)
4. 5 Domain Microservices (`:8081` - `:8085`)
5. Gateway Server (`:5000`)

---

### Option 2: Local Development via Maven
Build and run each service in sequence:
```bash
# 1. Compile & package all modules
mvn clean package -DskipTests

# 2. Run Eureka Server
cd eureka-server && mvn spring-boot:run

# 3. Run Config Server
cd ../config-server && mvn spring-boot:run

# 4. Run Domain Services
cd ../user-service && mvn spring-boot:run
cd ../lot-service && mvn spring-boot:run
cd ../demand-service && mvn spring-boot:run
cd ../trade-service && mvn spring-boot:run
cd ../market-service && mvn spring-boot:run

# 5. Run Gateway Server
cd ../gateway-server && mvn spring-boot:run
```

---

## 🛡️ Seamless Frontend Integration
The React Frontend (`frontend/`) connects to `http://localhost:5000/api/**` without any code changes:
- `http://localhost:5000/api/auth/**` & `api/users/**` $\to$ routed to `user-service`
- `http://localhost:5000/api/lots/**` & `api/inspections/**` $\to$ routed to `lot-service`
- `http://localhost:5000/api/demands/**` $\to$ routed to `demand-service`
- `http://localhost:5000/api/trades/**` $\to$ routed to `trade-service`
- `http://localhost:5000/api/market/**`, `api/analytics/**`, `api/logistics/**`, `api/notifications/**` $\to$ routed to `market-service`
