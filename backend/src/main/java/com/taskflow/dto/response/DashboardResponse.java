package com.taskflow.dto.response;

import java.util.Map;

public record DashboardResponse(long totalTasks, long totalResources,
    Map<String, Long> tasksByStatus, Map<String, Long> tasksByPriority,
    Map<String, Long> tasksByType, Map<String, Object> engineMetrics) {}
