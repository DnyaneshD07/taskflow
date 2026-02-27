package com.taskflow.repository;

import com.taskflow.domain.entity.AssignmentHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AssignmentHistoryRepository extends JpaRepository<AssignmentHistory, Long> {
    @Query("SELECT a FROM AssignmentHistory a WHERE a.task.id = :taskId ORDER BY a.assignedAt DESC")
    List<AssignmentHistory> findByTaskId(@Param("taskId") Long taskId);

    @Query("SELECT a FROM AssignmentHistory a WHERE a.task.id = :taskId AND a.unassignedAt IS NULL")
    Optional<AssignmentHistory> findActiveByTaskId(@Param("taskId") Long taskId);
}
