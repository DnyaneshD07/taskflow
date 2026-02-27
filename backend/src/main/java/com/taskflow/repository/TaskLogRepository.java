package com.taskflow.repository;

import com.taskflow.domain.entity.TaskLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface TaskLogRepository extends JpaRepository<TaskLog, Long> {
    @Query("SELECT l FROM TaskLog l WHERE l.task.id = :taskId ORDER BY l.occurredAt DESC")
    List<TaskLog> findByTaskId(@Param("taskId") Long taskId);
}
