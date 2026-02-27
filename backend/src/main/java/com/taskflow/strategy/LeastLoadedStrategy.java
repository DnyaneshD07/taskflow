package com.taskflow.strategy;

import com.taskflow.domain.entity.Resource;
import com.taskflow.domain.entity.Task;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/** Assigns to resource with lowest load factor. Ideal for balanced throughput. */
@Component("LEAST_LOADED")
public class LeastLoadedStrategy implements TaskAssignmentStrategy {

    @Override
    public Optional<Resource> selectResource(Task task, List<Resource> availableResources) {
        return availableResources.stream()
                .filter(Resource::hasCapacity)
                .min(Comparator.comparingDouble(Resource::loadFactor));
    }
}
