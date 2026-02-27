# ⚡ TaskFlow – Real-Time Distributed Task & Resource Management System

A production-grade, concurrent, backend-heavy system built with **Java 17 + Spring Boot + React**.

---

## Architecture Overview

```
taskflow/
├── backend/
│   └── src/main/java/com/taskflow/
│       ├── controller/          # REST layer (thin – delegates immediately to service)
│       ├── service/             # Business logic interfaces
│       │   └── impl/           # Concrete implementations
│       ├── repository/          # Spring Data JPA interfaces
│       ├── domain/
│       │   ├── entity/          # JPA entities (AbstractTask → Task, User, Resource, …)
│       │   └── enums/           # TaskStatus, TaskPriority, TaskType, ResourceType
│       ├── dto/
│       │   ├── request/         # Validated inbound records
│       │   └── response/        # Outbound records (ApiResponse<T>)
│       ├── concurrency/         # TaskAssignmentEngine (Producer-Consumer)
│       ├── strategy/            # TaskAssignmentStrategy + 3 implementations
│       ├── factory/             # TaskFactory
│       ├── security/
│       │   ├── jwt/             # JwtTokenProvider, JwtAuthenticationFilter
│       │   └── service/         # UserDetailsServiceImpl
│       ├── config/              # SecurityConfig, AssignmentStrategyConfig
│       └── exception/           # GlobalExceptionHandler, custom exceptions
├── frontend/
│   └── src/
│       └── App.jsx              # Full React SPA (Dashboard, Kanban, Analytics)
├── docker-compose.yml
└── README.md
```

---

## Key Design Decisions

### Concurrency Model
```
HTTP Thread (Producer)
       │
       ▼  submit(taskId)
  LinkedBlockingQueue<Long>  ──────── back-pressure buffer
       │
       ▼  poll() every 1s
  Consumer Thread (virtual)
       │
       ▼  submit(Runnable)
  ThreadPoolExecutor  ──────────────  [corePool=4, max=16]
       │
       ▼
  processAssignment(taskId)
    1. ReentrantLock(resourceId)   ← prevents intra-JVM race on same resource
    2. PESSIMISTIC_WRITE DB lock   ← prevents inter-JVM race (multi-pod)
    3. Re-validate capacity
    4. Assign & persist
    5. Unlock
```

### Race Condition Prevention
| Scenario | Prevention mechanism |
|---|---|
| Two threads assign same task | ConcurrentHashMap.newKeySet() in-flight tracker |
| Two threads overflow resource capacity | Per-resource ReentrantLock + pessimistic DB row lock |
| Dirty read on task version | @Version (optimistic locking) on Task entity |
| Duplicate user registration | DB UNIQUE constraint + application-level check |

### OOP Hierarchy
```
AbstractTask (MappedSuperclass)
  └── Task (Entity)                 -- slaMultiplier() = 1.5 for BUG_FIX, 1.0 otherwise

TaskAssignmentStrategy (Interface)
  ├── LeastLoadedStrategy           -- picks min loadFactor resource
  ├── RoundRobinStrategy            -- AtomicInteger cursor, no locks needed
  └── PriorityBasedStrategy         -- CRITICAL/HIGH → least loaded, LOW → most loaded
```

---

## API Reference

### Auth
| Method | Path | Body | Response |
|--------|------|------|----------|
| POST | `/api/auth/register` | `{username, email, password}` | `AuthResponse` |
| POST | `/api/auth/login`    | `{username, password}`        | `AuthResponse` |

### Tasks (JWT required)
| Method | Path | Description |
|--------|------|-------------|
| POST   | `/api/tasks`       | Create task (auto-queued for assignment) |
| GET    | `/api/tasks/{id}`  | Get task by ID |
| GET    | `/api/tasks/my`    | Paginated list of current user's tasks |
| PATCH  | `/api/tasks/{id}`  | Update task (partial) |
| DELETE | `/api/tasks/{id}`  | Delete task (not IN_PROGRESS) |

### Dashboard
| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/dashboard` | Stats + engine metrics |

### Request / Response format
```json
// POST /api/tasks
{
  "title": "Fix login bug",
  "type": "BUG_FIX",
  "priority": "HIGH",
  "deadline": "2026-03-01T18:00:00",
  "estimatedHours": 4
}

// Response envelope
{
  "success": true,
  "message": "Task created",
  "data": { "id": 42, "status": "TODO", "resourceName": null, ... }
}
```

---

## Running Locally

### Option A – Docker Compose (recommended)
```bash
git clone <repo>
cd taskflow

# Start everything (MySQL + backend + frontend)
docker-compose up --build

# App available at:
# Frontend  → http://localhost:3000
# API       → http://localhost:8080/api
```

### Option B – Manual
**Prerequisites:** Java 17, Maven, Node 20, MySQL 8

```bash
# 1. Create database
mysql -u root -p -e "CREATE DATABASE taskflow; CREATE USER 'taskflow'@'localhost' IDENTIFIED BY 'secret'; GRANT ALL ON taskflow.* TO 'taskflow'@'localhost';"

# 2. Start backend
cd taskflow/backend
mvn spring-boot:run

# 3. Start frontend
cd taskflow/frontend
npm install
npm run dev
# → http://localhost:3000
```

### Environment Variables
| Variable | Default | Description |
|----------|---------|-------------|
| `DB_HOST` | `localhost` | MySQL host |
| `DB_NAME` | `taskflow` | Database name |
| `DB_USER` | `root` | DB username |
| `DB_PASS` | `secret` | DB password |
| `JWT_SECRET` | (see yml) | 32+ char secret |
| `TASKFLOW_ASSIGNMENT_STRATEGY` | `LEAST_LOADED` | `LEAST_LOADED \| ROUND_ROBIN \| PRIORITY_BASED` |

---

## Performance Characteristics
- **HikariCP** pool: 5–20 connections
- **ThreadPoolExecutor**: 4 core / 16 max workers
- **Optimistic locking** on tasks (`@Version`) prevents lost updates without blocking
- **Composite index** `(status, priority)` accelerates the assignment engine's polling query
- `JOIN FETCH` on all multi-entity queries eliminates N+1

---

## Testing
```bash
cd backend
mvn test                        # unit + slice tests
mvn test -Dtest=ConcurrencyTest # dedicated concurrency tests
```
