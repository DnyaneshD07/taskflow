package com.taskflow.dto.request;

import com.taskflow.domain.enums.TaskPriority;
import com.taskflow.domain.enums.TaskType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CreateTaskRequest(
    @NotBlank @Size(max = 255) String title,
    String description,
    @NotNull TaskType type,
    TaskPriority priority,
    LocalDateTime deadline,
    BigDecimal estimatedHours
) {}
