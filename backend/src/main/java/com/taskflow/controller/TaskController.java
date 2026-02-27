package com.taskflow.controller;

import com.taskflow.dto.request.CreateTaskRequest;
import com.taskflow.dto.request.UpdateTaskRequest;
import com.taskflow.dto.response.ApiResponse;
import com.taskflow.dto.response.TaskResponse;
import com.taskflow.service.TaskService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;

    @PostMapping
    public ResponseEntity<ApiResponse<TaskResponse>> create(
        @Valid @RequestBody CreateTaskRequest req,
        @AuthenticationPrincipal UserDetails principal
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.ok("Task created", taskService.create(req, principal.getUsername())));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<TaskResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(taskService.findById(id)));
    }

    @GetMapping("/my")
    public ResponseEntity<ApiResponse<Page<TaskResponse>>> getMyTasks(
        @RequestParam(defaultValue = "0")  int page,
        @RequestParam(defaultValue = "20") int size,
        @AuthenticationPrincipal UserDetails principal
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(ApiResponse.ok(taskService.findByCurrentUser(principal.getUsername(), pageable)));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponse<TaskResponse>> update(
        @PathVariable Long id,
        @Valid @RequestBody UpdateTaskRequest req,
        @AuthenticationPrincipal UserDetails principal
    ) {
        return ResponseEntity.ok(ApiResponse.ok("Task updated",
            taskService.update(id, req, principal.getUsername())));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(
        @PathVariable Long id,
        @AuthenticationPrincipal UserDetails principal
    ) {
        taskService.delete(id, principal.getUsername());
        return ResponseEntity.ok(ApiResponse.ok("Task deleted", null));
    }
}
