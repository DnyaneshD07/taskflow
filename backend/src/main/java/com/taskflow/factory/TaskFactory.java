package com.taskflow.factory;

import com.taskflow.domain.entity.Task;
import com.taskflow.domain.entity.User;
import com.taskflow.domain.enums.TaskPriority;
import com.taskflow.domain.enums.TaskStatus;
import com.taskflow.domain.enums.TaskType;
import com.taskflow.dto.request.CreateTaskRequest;
import org.springframework.stereotype.Component;

/**
 * Centralises Task construction so callers never touch new Task() directly.
 * Adding a new task variant requires only a new branch here (OCP-friendly).
 */
@Component
public class TaskFactory {

    public Task create(CreateTaskRequest req, User creator) {
        Task task = new Task();
        task.setTitle(req.title());
        task.setDescription(req.description());
        task.setType(req.type());
        task.setStatus(TaskStatus.TODO);
        task.setPriority(req.priority() != null ? req.priority() : TaskPriority.MEDIUM);
        task.setDeadline(req.deadline());
        task.setEstimatedHours(req.estimatedHours());
        task.setCreator(creator);

        // Type-specific defaults
        if (req.type() == TaskType.BUG_FIX && task.getPriority().getWeight() < TaskPriority.HIGH.getWeight()) {
            // Bug fixes are auto-elevated to at least HIGH priority
            task.setPriority(TaskPriority.HIGH);
        }

        return task;
    }
}
