package com.taskflow.config;

import com.taskflow.strategy.TaskAssignmentStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Slf4j
@Configuration
public class AssignmentStrategyConfig {

    @Bean
    @org.springframework.context.annotation.Primary
    public TaskAssignmentStrategy taskAssignmentStrategy(
        @Value("${taskflow.assignment.strategy:LEAST_LOADED}") String strategyName,
        Map<String, TaskAssignmentStrategy> strategies
    ) {
        TaskAssignmentStrategy strategy = strategies.get(strategyName);
        if (strategy == null) {
            log.warn("Unknown strategy '{}', falling back to LEAST_LOADED", strategyName);
            strategy = strategies.get("LEAST_LOADED");
        }
        log.info("Task assignment strategy: {}", strategyName);
        return strategy;
    }
}
