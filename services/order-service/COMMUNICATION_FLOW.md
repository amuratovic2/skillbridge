# Order-Service ↔ Gig-Service: Communication Flow Diagram

## Non-trivial synchronous communication: Create Order

**Why it is non-trivial:**  
The result depends on the state of *two* databases simultaneously:
- `gig-service` DB must contain an ACTIVE gig with a current price and seller
- `order-service` DB must not already hold a duplicate order

Price, seller ID, delivery deadline and revision count are **read from gig-service's DB**
and written into `order-service`'s DB in the same logical operation.

---

### Happy path

```mermaid
sequenceDiagram
    actor Client
    participant OS as order-service<br/>(port 3003)
    participant Eureka as Eureka Server<br/>(port 8761)
    participant GS as gig-service<br/>(port 3002)
    participant ODB as orders DB<br/>(PostgreSQL)
    participant GDB as gigs DB<br/>(PostgreSQL)

    Client->>OS: POST /api/orders<br/>{ gigId: 42 }

    Note over OS: @LoadBalanced RestTemplate<br/>resolves "gig-service" hostname
    OS->>Eureka: Where is gig-service?
    Eureka-->>OS: host:port (e.g. 192.168.1.10:3002)

    OS->>GS: GET /api/gigs/42
    GS->>GDB: SELECT * FROM gigs WHERE id = 42
    GDB-->>GS: { id:42, status:ACTIVE, cost:150, freelancerId:5, deliveryTime:7, revisionCount:3 }
    GS-->>OS: 200 OK { success:true, data: { ... } }

    Note over OS: Validates status == ACTIVE<br/>Maps gig fields → order fields

    OS->>ODB: INSERT INTO orders (clientId, gigId, sellerId, totalCost, ...)
    ODB-->>OS: saved Order { id: 1, status: PENDING }
    OS-->>Client: 201 Created { orderId: 1, status: PENDING, totalCost: 150 }
```

---

### Gig not found (404)

```mermaid
sequenceDiagram
    actor Client
    participant OS as order-service
    participant GS as gig-service

    Client->>OS: POST /api/orders { gigId: 999 }
    OS->>GS: GET /api/gigs/999
    GS-->>OS: 404 Not Found
    Note over OS: HttpClientErrorException.NotFound caught<br/>Circuit breaker NOT triggered
    OS-->>Client: 404 { error: "Gig with id 999 not found" }
```

---

### Gig exists but is not ACTIVE (e.g. PAUSED)

```mermaid
sequenceDiagram
    actor Client
    participant OS as order-service
    participant GS as gig-service

    Client->>OS: POST /api/orders { gigId: 7 }
    OS->>GS: GET /api/gigs/7
    GS-->>OS: 200 OK { status: "PAUSED", ... }
    Note over OS: status ≠ ACTIVE → reject
    OS-->>Client: 400 Bad Request { error: "Gig nije dostupan za narudžbu (status: PAUSED)" }
```

---

### gig-service is DOWN — circuit CLOSED (first failures)

```mermaid
sequenceDiagram
    actor Client
    participant OS as order-service
    participant CB as Circuit Breaker<br/>(Resilience4j)
    participant GS as gig-service<br/>(DOWN)

    Client->>OS: POST /api/orders { gigId: 1 }
    OS->>CB: call getGig(1)
    CB->>GS: GET /api/gigs/1
    GS--xCB: Connection refused (ResourceAccessException)
    Note over CB: Counts failure.<br/>If failure rate > 50% in last 5 calls → OPEN circuit
    CB-->>OS: ResourceAccessException propagates
    OS-->>Client: 503 Service Unavailable<br/>"Gig service is currently unavailable"
```

---

### gig-service is DOWN — circuit OPEN (fail fast)

```mermaid
sequenceDiagram
    actor Client
    participant OS as order-service
    participant CB as Circuit Breaker<br/>(OPEN)

    Client->>OS: POST /api/orders { gigId: 1 }
    OS->>CB: call getGig(1)
    Note over CB: Circuit is OPEN.<br/>Does NOT call gig-service at all.
    CB-->>OS: CallNotPermittedException → fallback
    Note over OS: getGigFallback() called<br/>Returns 503 immediately
    OS-->>Client: 503 Service Unavailable<br/>"Gig service is temporarily unavailable. Please try again later."

    Note over CB: After 10s wait → HALF-OPEN<br/>Lets one probe through
```

---

### Load balancing across multiple gig-service instances

```mermaid
sequenceDiagram
    actor Client
    participant OS as order-service
    participant LB as Spring Cloud LoadBalancer<br/>(Round-Robin)
    participant GS1 as gig-service instance 1<br/>(192.168.1.10:3002)
    participant GS2 as gig-service instance 2<br/>(192.168.1.11:3002)

    Client->>OS: POST /api/orders { gigId: 1 }
    OS->>LB: resolve "gig-service"
    LB-->>OS: → instance 1
    OS->>GS1: GET /api/gigs/1
    GS1-->>OS: 200 OK
    OS-->>Client: 201 Created

    Client->>OS: POST /api/orders { gigId: 2 }
    OS->>LB: resolve "gig-service"
    LB-->>OS: → instance 2  (round-robin)
    OS->>GS2: GET /api/gigs/2
    GS2-->>OS: 200 OK
    OS-->>Client: 201 Created
```

---

## Summary of design decisions

| Problem | Solution |
|---------|----------|
| Tight coupling (sync) | Circuit breaker: order-service stays up even when gig-service is down |
| Hardcoded IP/port | `@LoadBalanced` RestTemplate + Eureka service discovery |
| Slow gig-service | 3 s connect / 5 s read timeout on RestTemplate |
| Price manipulation by client | Price, sellerId, revisions fetched from gig-service — not accepted from request body |
| 404 opening circuit | `recordExceptions` includes only `ResourceAccessException` and 5xx, not 4xx |
