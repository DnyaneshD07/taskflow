package com.taskflow.service.impl;

import com.taskflow.concurrency.TaskAssignmentEngine;
import com.taskflow.domain.entity.Task;
import com.taskflow.domain.entity.TaskLog;
import com.taskflow.domain.entity.User;
import com.taskflow.domain.enums.TaskStatus;
import com.taskflow.dto.request.CreateTaskRequest;
import com.taskflow.dto.request.UpdateTaskRequest;
import com.taskflow.dto.response.TaskResponse;
import com.taskflow.exception.ConflictException;
import com.taskflow.exception.ResourceNotFoundException;
import com.taskflow.factory.TaskFactory;
import com.taskflow.repository.TaskLogRepository;
import com.taskflow.repository.TaskRepository;
import com.taskflow.repository.UserRepository;
import com.taskflow.service.TaskService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class TaskServiceImpl implements TaskService {

    private final TaskRepository        taskRepository;
    private final UserRepository        userRepository;
    private final TaskLogRepository     logRepository;
    private final TaskFactory           taskFactory;
    private final TaskAssignmentEngine  assignmentEngine;

    @Override
    @Transactional
    public TaskResponse create(CreateTaskRequest request, String username) {
        User creator = userRepository.findByUsername(username)
            .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));

        Task task = taskFactory.create(request, creator);
        task = taskRepository.save(task);

        logRepository.save(TaskLog.of(task, username, "CREATED", null));

        // Non-blocking hand-off to the assignment engine
        assignmentEngine.submit(task.getId());

        log.info("Task {} created by {} – submitted to assignment engine", task.getId(), username);
        return toResponse(task);
    }

    @Override
    @Transactional
    public TaskResponse update(Long id, UpdateTaskRequest request, String username) {
        Task task = taskRepository.findById(id)
            .orElseThrow(() -> ResourceNotFoundException.task(id));

        requireOwnerOrAdmin(task, username);

        if (request.title()          != null) task.setTitle(request.title());
        if (request.description()    != null) task.setDescription(request.description());
        if (request.priority()       != null) task.setPriority(request.priority());
        if (request.deadline()       != null) task.setDeadline(request.deadline());
        if (request.estimatedHours() != null) task.setEstimatedHours(request.estimatedHours());

        if (request.status() != null) {
            validateStatusTransition(task.getStatus(), request.status());
            task.setStatus(request.status());

            // Release resource capacity if task reaches terminal state
            if (isTerminal(request.status()) && task.getResource() != null) {
                var resource = task.getResource();
                resource.setActiveTasks(Math.max(0, resource.getActiveTasks() - 1));
                task.setResource(null);
            }
        }

        task = taskRepository.save(task);
        logRepository.save(TaskLog.of(task, username, "UPDATED", "status=" + task.getStatus()));
        return toResponse(task);
    }

    @Override
    @Transactional
    public void delete(Long id, String username) {
        Task task = taskRepository.findById(id)
            .orElseThrow(() -> ResourceNotFoundException.task(id));
        requireOwnerOrAdmin(task, username);

        if (task.getStatus() == TaskStatus.IN_PROGRESS) {
            throw new ConflictException("Cannot delete an in-progress task. Complete or cancel it first.");
        }
        taskRepository.delete(task);
        log.info("Task {} deleted by {}", id, username);
    }

    @Override
    @Transactional(readOnly = true)
    public TaskResponse findById(Long id) {
        return toResponse(taskRepository.findById(id)
            .orElseThrow(() -> ResourceNotFoundException.task(id)));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TaskResponse> findByCurrentUser(String username, Pageable pageable) {
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));
        return taskRepository.findByCreatorId(user.getId(), pageable)
            .map(this::toResponse);
    }

    // ─── Helpers ────────────────────────────────────────────────────────────

    private void requireOwnerOrAdmin(Task task, String username) {
        boolean isOwner = task.getCreator().getUsername().equals(username);
        boolean isAdmin = userRepository.findByUsername(username)
            .map(u -> u.getRoles().stream().anyMatch(r -> r.getName().equals("ROLE_ADMIN")))
            .orElse(false);
        if (!isOwner && !isAdmin) throw new AccessDeniedException("Not authorised to modify this task");
    }

    private void validateStatusTransition(TaskStatus from, TaskStatus to) {
        boolean invalid = switch (from) {
            case COMPLETED, CANCELLED, FAILED -> true;
            case TODO        -> to == TaskStatus.COMPLETED;
            default          -> false;
        };
        if (invalid) {
            throw new ConflictException("Invalid status transition: " + from + " → " + to);
        }
    }

    private boolean isTerminal(TaskStatus status) {
        return status == TaskStatus.COMPLETED
            || status == TaskStatus.CANCELLED
            || status == TaskStatus.FAILED;
    }

    private TaskResponse toResponse(Task t) {
        return new TaskResponse(
            t.getId(), t.getTitle(), t.getDescription(), t.getType(),
            t.getStatus(), t.getPriority(), t.getDeadline(), t.getEstimatedHours(),
            t.getCreator() != null ? t.getCreator().getUsername() : null,
            t.getResource() != null ? t.getResource().getName()   : null,
            t.getCreatedAt(), t.getUpdatedAt()
        );
    }
}
