package com.taskflow.service.impl;

import com.taskflow.concurrency.TaskAssignmentEngine;
import com.taskflow.dto.response.DashboardResponse;
import com.taskflow.repository.ResourceRepository;
import com.taskflow.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardServiceImpl {

    private final TaskRepository       taskRepository;
    private final ResourceRepository   resourceRepository;
    private final TaskAssignmentEngine assignmentEngine;

    @Transactional(readOnly = true)
    public DashboardResponse getDashboard() {
        return new DashboardResponse(
            taskRepository.count(),
            resourceRepository.count(),
            aggregateByLabel(taskRepository.countByStatus()),
            aggregateByLabel(taskRepository.countByPriority()),
            aggregateByLabel(taskRepository.countByType()),
            assignmentEngine.metrics()
        );
    }

    private Map<String, Long> aggregateByLabel(List<Object[]> rows) {
        return rows.stream().collect(Collectors.toMap(
            r -> r[0].toString(),
            r -> (Long) r[1],
            (a, b) -> a,
            LinkedHashMap::new
        ));
    }
}
