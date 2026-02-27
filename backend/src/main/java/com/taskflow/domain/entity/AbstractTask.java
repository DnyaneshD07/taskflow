package com.taskflow.domain.entity;

import com.taskflow.domain.enums.TaskPriority;
import com.taskflow.domain.enums.TaskStatus;
import com.taskflow.domain.enums.TaskType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Abstract base for all task variants.
 * SINGLE_TABLE strategy maximises query performance – no joins needed.
 * Subclasses override behaviour (polymorphism) while sharing the schema.
 */
@Getter
@Setter
@MappedSuperclass
public abstract class AbstractTask {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TaskType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TaskStatus status = TaskStatus.TODO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private TaskPriority priority = TaskPriority.MEDIUM;

    private LocalDateTime deadline;

    @Column(precision = 6, scale = 2)
    private BigDecimal estimatedHours;

    @Version
    private Long version;   // Optimistic locking – prevents lost-update race conditions

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    /**
     * Each concrete task type knows its SLA multiplier.
     * Subclasses override to influence scheduling weight.
     */
    public abstract double slaMultiplier();

    /**
     * Computed scheduling score – higher means more urgent.
     * Combines priority weight with SLA pressure.
     */
    public double schedulingScore() {
        double base = priority.getWeight() * slaMultiplier();
        if (deadline != null) {
            long hoursUntilDeadline = java.time.Duration.between(LocalDateTime.now(), deadline).toHours();
            double urgency = hoursUntilDeadline <= 0 ? 10 : 24.0 / Math.max(hoursUntilDeadline, 1);
            base += urgency;
        }
        return base;
    }
}
