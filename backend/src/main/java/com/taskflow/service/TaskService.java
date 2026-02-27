package com.taskflow.service;

import com.taskflow.dto.request.CreateTaskRequest;
import com.taskflow.dto.request.UpdateTaskRequest;
import com.taskflow.dto.response.TaskResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface TaskService {
    TaskResponse  create(CreateTaskRequest request, String username);
    TaskResponse  update(Long id, UpdateTaskRequest request, String username);
    void          delete(Long id, String username);
    TaskResponse  findById(Long id);
    Page<TaskResponse> findByCurrentUser(String username, Pageable pageable);
}
