package com.taskflow.concurrency;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@EnableScheduling
@RequiredArgsConstructor
public class AssignmentSweepScheduler {

    private final TaskAssignmentEngine engine;

    /** Every 30s, re-queue any stranded TODO tasks. */
    @Scheduled(fixedDelay = 30_000, initialDelay = 10_000)
    public void sweep() {
        log.debug("Running unassigned task sweep");
        engine.sweepUnassigned();
    }
}
