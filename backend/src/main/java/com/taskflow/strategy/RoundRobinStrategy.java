package com.taskflow.strategy;

import com.taskflow.domain.entity.Resource;
import com.taskflow.domain.entity.Task;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Simple round-robin across available resources.
 * AtomicInteger is used instead of a synchronized int to avoid lock overhead
 * while still guaranteeing visibility and atomicity of the counter.
 */
@Component("ROUND_ROBIN")
public class RoundRobinStrategy implements TaskAssignmentStrategy {

    // AtomicInteger avoids synchronization overhead for a simple increment
    private final AtomicInteger cursor = new AtomicInteger(0);

    @Override
    public Optional<Resource> selectResource(Task task, List<Resource> availableResources) {
        List<Resource> capable = availableResources.stream()
                .filter(Resource::hasCapacity)
                .toList();
        if (capable.isEmpty()) return Optional.empty();

        // getAndIncrement + mod: thread-safe index rotation without locks
        int index = cursor.getAndIncrement() % capable.size();
        return Optional.of(capable.get(index));
    }
}
