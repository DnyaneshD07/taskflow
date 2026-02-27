package com.taskflow.exception;

public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) { super(message); }
    public static ResourceNotFoundException task(Long id)     { return new ResourceNotFoundException("Task not found: " + id); }
    public static ResourceNotFoundException resource(Long id) { return new ResourceNotFoundException("Resource not found: " + id); }
}
