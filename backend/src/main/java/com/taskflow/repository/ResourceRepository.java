package com.taskflow.repository;

import com.taskflow.domain.entity.Resource;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ResourceRepository extends JpaRepository<Resource, Long> {

    /**
     * Pessimistic write lock on the resource row prevents two threads
     * from concurrently assigning tasks to the same resource and
     * exceeding its capacity.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM Resource r WHERE r.id = :id")
    Optional<Resource> findByIdWithLock(Long id);

    @Query("SELECT r FROM Resource r WHERE r.available = true AND r.activeTasks < r.capacity ORDER BY r.activeTasks ASC")
    List<Resource> findAvailableOrderedByLoad();
}
