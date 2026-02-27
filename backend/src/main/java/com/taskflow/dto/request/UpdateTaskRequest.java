package com.taskflow.dto.request;

import com.taskflow.domain.enums.TaskPriority;
import com.taskflow.domain.enums.TaskStatus;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record UpdateTaskRequest(
    @Size(max = 255) String title,
    String description,
    TaskStatus status,
    TaskPriority priority,
    LocalDateTime deadline,
    BigDecimal estimatedHours
) {}
