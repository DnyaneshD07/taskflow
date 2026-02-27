package com.taskflow.repository;

import com.taskflow.domain.entity.Task;
import com.taskflow.domain.enums.TaskStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TaskRepository extends JpaRepository<Task, Long> {

    /**
     * Fetch unassigned tasks ordered by scheduling score proxy (priority + deadline).
     * Uses JOIN FETCH to prevent N+1 on creator.
     */
    @Query("""
        SELECT t FROM Task t
        JOIN FETCH t.creator
        WHERE t.status = :status AND t.resource IS NULL
        ORDER BY t.priority DESC, t.deadline ASC NULLS LAST
    """)
    List<Task> findUnassignedByStatus(@Param("status") TaskStatus status);

    @Query("""
        SELECT t FROM Task t
        JOIN FETCH t.creator
        LEFT JOIN FETCH t.resource
        WHERE t.creator.id = :userId
    """)
    Page<Task> findByCreatorId(@Param("userId") Long userId, Pageable pageable);

    @Query("""
        SELECT t FROM Task t
        JOIN FETCH t.creator
        LEFT JOIN FETCH t.resource
        WHERE t.resource.id = :resourceId AND t.status = :status
    """)
    List<Task> findByResourceAndStatus(@Param("resourceId") Long resourceId,
                                       @Param("status") TaskStatus status);

    // Dashboard stats queries
    @Query("SELECT t.status, COUNT(t) FROM Task t GROUP BY t.status")
    List<Object[]> countByStatus();

    @Query("SELECT t.priority, COUNT(t) FROM Task t GROUP BY t.priority")
    List<Object[]> countByPriority();

    @Query("SELECT t.type, COUNT(t) FROM Task t GROUP BY t.type")
    List<Object[]> countByType();
}
