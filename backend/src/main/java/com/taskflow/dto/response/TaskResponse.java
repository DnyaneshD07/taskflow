package com.taskflow.dto.response;

import com.taskflow.domain.enums.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TaskResponse(Long id, String title, String description, TaskType type,
    TaskStatus status, TaskPriority priority, LocalDateTime deadline, BigDecimal estimatedHours,
    String creatorUsername, String resourceName, LocalDateTime createdAt, LocalDateTime updatedAt) {}
