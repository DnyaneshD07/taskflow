# ⚡ TaskFlow – Real-Time Distributed Task & Resource Management System

:

🚀 About TaskFlow

TaskFlow is a modern, scalable task management application designed to streamline productivity through an intuitive and efficient workflow system. Built with a focus on performance, usability, and clean architecture, TaskFlow enables users to organize, prioritize, and track tasks seamlessly in real time.

The application implements a structured task lifecycle with features such as task creation, status tracking, prioritization, and dynamic updates, ensuring users can manage both simple to complex workflows efficiently. It is designed with modular components and optimized state management, making it highly maintainable and extensible for future enhancements.

From a technical perspective, TaskFlow emphasizes clean code practices, responsive UI/UX, and robust backend integration, showcasing strong fundamentals in full-stack development. The project demonstrates the ability to design scalable systems, handle real-world use cases, and build production-ready applications.

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

🚀 **Zero Database Setup Needed!**
The project runs entirely using an embedded **H2 in-memory database**. There is absolutely no need to install or run MySQL/Docker to develop locally! 

**Prerequisites:** Java 17, Maven, Node 20+

```bash
# 1. Start backend
cd taskflow/backend
mvn spring-boot:run
# → API goes live at http://localhost:8080/api

# 2. Start frontend
cd taskflow/frontend
npm install
npm run dev
# → UI goes live at http://localhost:3000
```

*(Note: Data is automatically seeded on startup using `data.sql` and resets when the backend stops.)*

### Option B – Docker Compose
If you prefer running everything together with a standard persistent MySQL database:
```bash
docker-compose up --build
```

### Environment Variables
| Variable | Default | Description |
|----------|---------|-------------|
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
