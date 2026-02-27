package com.taskflow.strategy;

import com.taskflow.domain.entity.Resource;
import com.taskflow.domain.entity.Task;

import java.util.List;
import java.util.Optional;

/**
 * Strategy interface for task-to-resource assignment.
 * Implementations are swapped via config without touching calling code (OCP).
 */
public interface TaskAssignmentStrategy {
    /**
     * Select the best available resource for the given task.
     *
     * @param task               the task to be assigned
     * @param availableResources snapshot of resources with remaining capacity
     * @return the chosen resource, or empty if none suitable
     */
    Optional<Resource> selectResource(Task task, List<Resource> availableResources);
}
