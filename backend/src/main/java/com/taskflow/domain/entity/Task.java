package com.taskflow.domain.entity;

import com.taskflow.domain.enums.TaskType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Persisted Task entity.
 *
 * BugFixTask and FeatureTask are logical subtypes expressed via the
 * factory + strategy pattern rather than separate tables, keeping
 * the schema lean while preserving full OOP polymorphism in the domain.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(
    name = "tasks",
    indexes = {
        @Index(name = "idx_tasks_status_priority", columnList = "status, priority"),
        @Index(name = "idx_tasks_deadline",        columnList = "deadline")
    }
)
public class Task extends AbstractTask {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creator_id", nullable = false)
    private User creator;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resource_id")
    private Resource resource;

    /** BUG_FIX tasks have 1.5× SLA pressure by default */
    @Override
    public double slaMultiplier() {
        return getType() == TaskType.BUG_FIX ? 1.5 : 1.0;
    }
}
