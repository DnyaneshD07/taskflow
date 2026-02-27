package com.taskflow.domain.entity;

import com.taskflow.domain.enums.ResourceType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Represents a worker, machine, or service that tasks can be assigned to.
 *
 * activeTasks is the hot counter used by the assignment engine.
 * It is incremented/decremented inside a DB transaction to prevent
 * double-assignment race conditions at the persistence layer.
 */
@Getter @Setter @NoArgsConstructor
@Entity
@Table(name = "resources")
public class Resource {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ResourceType type = ResourceType.HUMAN;

    @Column(nullable = false)
    private int capacity = 5;

    @Column(name = "active_tasks", nullable = false)
    private int activeTasks = 0;

    @Column(nullable = false)
    private boolean available = true;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    public boolean hasCapacity() {
        return available && activeTasks < capacity;
    }

    /** Load factor in [0,1]. Lower = less loaded. */
    public double loadFactor() {
        return capacity == 0 ? 1.0 : (double) activeTasks / capacity;
    }
}
