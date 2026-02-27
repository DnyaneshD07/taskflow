package com.taskflow.strategy;

import com.taskflow.domain.entity.Resource;
import com.taskflow.domain.entity.Task;
import com.taskflow.domain.enums.TaskPriority;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * CRITICAL and HIGH priority tasks get the least-loaded resource.
 * Lower priority tasks are directed to the most-loaded (but still capable)
 * resource to consolidate work and free up headroom for urgent tasks.
 */
@Component("PRIORITY_BASED")
public class PriorityBasedStrategy implements TaskAssignmentStrategy {

    @Override
    public Optional<Resource> selectResource(Task task, List<Resource> availableResources) {
        List<Resource> capable = availableResources.stream()
                .filter(Resource::hasCapacity)
                .toList();
        if (capable.isEmpty()) return Optional.empty();

        boolean isUrgent = task.getPriority() == TaskPriority.CRITICAL
                        || task.getPriority() == TaskPriority.HIGH;

        return isUrgent
                ? capable.stream().min(Comparator.comparingDouble(Resource::loadFactor))
                : capable.stream().max(Comparator.comparingDouble(Resource::loadFactor));
    }
}
