-- ============================================================
-- TaskFlow – Real-Time Task & Resource Management System
-- Production-grade MySQL Schema
-- ============================================================

SET FOREIGN_KEY_CHECKS = 0;
SET NAMES utf8mb4;

-- ============================================================
-- ROLES
-- ============================================================
CREATE TABLE IF NOT EXISTS roles (
    id          BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT,
    name        VARCHAR(50)      NOT NULL,
    created_at  DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uq_roles_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- USERS
-- ============================================================
CREATE TABLE IF NOT EXISTS users (
    id              BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT,
    username        VARCHAR(80)      NOT NULL,
    email           VARCHAR(255)     NOT NULL,
    password_hash   VARCHAR(255)     NOT NULL,
    enabled         TINYINT(1)       NOT NULL DEFAULT 1,
    created_at      DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uq_users_username (username),
    UNIQUE KEY uq_users_email   (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- USER_ROLES (join table)
-- ============================================================
CREATE TABLE IF NOT EXISTS user_roles (
    user_id BIGINT UNSIGNED NOT NULL,
    role_id BIGINT UNSIGNED NOT NULL,
    PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_ur_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_ur_role FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- RESOURCES
-- Represents workers / machines that tasks are assigned to
-- ============================================================
CREATE TABLE IF NOT EXISTS resources (
    id              BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT,
    name            VARCHAR(120)     NOT NULL,
    type            ENUM('HUMAN','MACHINE','SERVICE') NOT NULL DEFAULT 'HUMAN',
    capacity        INT UNSIGNED     NOT NULL DEFAULT 5,   -- max concurrent tasks
    active_tasks    INT UNSIGNED     NOT NULL DEFAULT 0,   -- maintained by app
    available       TINYINT(1)       NOT NULL DEFAULT 1,
    created_at      DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_resources_available (available),
    INDEX idx_resources_active_tasks (active_tasks),
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- TASKS
-- ============================================================
CREATE TABLE IF NOT EXISTS tasks (
    id              BIGINT UNSIGNED      NOT NULL AUTO_INCREMENT,
    title           VARCHAR(255)         NOT NULL,
    description     TEXT,
    type            ENUM('BUG_FIX','FEATURE','MAINTENANCE','INVESTIGATION') NOT NULL,
    status          ENUM('TODO','IN_PROGRESS','COMPLETED','CANCELLED','FAILED') NOT NULL DEFAULT 'TODO',
    priority        ENUM('LOW','MEDIUM','HIGH','CRITICAL') NOT NULL DEFAULT 'MEDIUM',
    deadline        DATETIME             NULL,
    estimated_hours DECIMAL(6,2)         NULL,
    creator_id      BIGINT UNSIGNED      NOT NULL,
    resource_id     BIGINT UNSIGNED      NULL,
    version         BIGINT UNSIGNED      NOT NULL DEFAULT 0,   -- optimistic locking
    created_at      DATETIME             NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME             NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_tasks_creator  FOREIGN KEY (creator_id)  REFERENCES users(id),
    CONSTRAINT fk_tasks_resource FOREIGN KEY (resource_id) REFERENCES resources(id) ON DELETE SET NULL,
    INDEX idx_tasks_status   (status),
    INDEX idx_tasks_priority (priority),
    INDEX idx_tasks_creator  (creator_id),
    INDEX idx_tasks_resource (resource_id),
    INDEX idx_tasks_deadline (deadline),
    INDEX idx_tasks_status_priority (status, priority)  -- composite for assignment queries
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- ASSIGNMENT_HISTORY
-- Immutable audit trail of every task<->resource assignment
-- ============================================================
CREATE TABLE IF NOT EXISTS assignment_history (
    id              BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT,
    task_id         BIGINT UNSIGNED  NOT NULL,
    resource_id     BIGINT UNSIGNED  NOT NULL,
    assigned_at     DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP,
    unassigned_at   DATETIME         NULL,
    assigned_by     VARCHAR(80)      NOT NULL,   -- username or 'SYSTEM'
    PRIMARY KEY (id),
    CONSTRAINT fk_ah_task     FOREIGN KEY (task_id)     REFERENCES tasks(id),
    CONSTRAINT fk_ah_resource FOREIGN KEY (resource_id) REFERENCES resources(id),
    INDEX idx_ah_task     (task_id),
    INDEX idx_ah_resource (resource_id),
    INDEX idx_ah_assigned_at (assigned_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- TASK_LOGS
-- Fine-grained lifecycle log per task
-- ============================================================
CREATE TABLE IF NOT EXISTS task_logs (
    id          BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT,
    task_id     BIGINT UNSIGNED  NOT NULL,
    actor       VARCHAR(80)      NOT NULL,
    event       VARCHAR(80)      NOT NULL,
    detail      TEXT             NULL,
    occurred_at DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_tl_task FOREIGN KEY (task_id) REFERENCES tasks(id) ON DELETE CASCADE,
    INDEX idx_tl_task       (task_id),
    INDEX idx_tl_occurred   (occurred_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

SET FOREIGN_KEY_CHECKS = 1;

-- ============================================================
-- SEED DATA
-- ============================================================
INSERT IGNORE INTO roles (name) VALUES ('ROLE_USER'), ('ROLE_ADMIN');

INSERT IGNORE INTO resources (name, type, capacity) VALUES
  ('Alpha Team',   'HUMAN',   8),
  ('Beta Team',    'HUMAN',   6),
  ('CI Pipeline',  'MACHINE', 20),
  ('QA Agent',     'SERVICE', 10);
