package com.taskflow.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor
@Entity
@Table(name = "task_logs")
public class TaskLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id", nullable = false)
    private Task task;

    @Column(nullable = false, length = 80)
    private String actor;

    @Column(nullable = false, length = 80)
    private String event;

    @Column(columnDefinition = "TEXT")
    private String detail;

    @CreationTimestamp
    private LocalDateTime occurredAt;

    public static TaskLog of(Task task, String actor, String event, String detail) {
        TaskLog log = new TaskLog();
        log.setTask(task);
        log.setActor(actor);
        log.setEvent(event);
        log.setDetail(detail);
        return log;
    }
}
