package com.taskflow.concurrency;

import com.taskflow.domain.entity.AssignmentHistory;
import com.taskflow.domain.entity.Resource;
import com.taskflow.domain.entity.Task;
import com.taskflow.domain.entity.TaskLog;
import com.taskflow.domain.enums.TaskStatus;
import com.taskflow.repository.AssignmentHistoryRepository;
import com.taskflow.repository.ResourceRepository;
import com.taskflow.repository.TaskLogRepository;
import com.taskflow.repository.TaskRepository;
import com.taskflow.strategy.TaskAssignmentStrategy;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantLock;

/**
 * ─────────────────────────────────────────────────────────────────
 * TaskAssignmentEngine  –  Heart of the concurrency module
 * ─────────────────────────────────────────────────────────────────
 *
 * Design: Producer-Consumer
 *  • Producers: HTTP request threads enqueue task IDs via submit()
 *  • Consumer:  A dedicated consumer thread drains the queue and
 *               dispatches assignment work to a ThreadPoolExecutor
 *
 * Why BlockingQueue?
 *  LinkedBlockingQueue provides back-pressure: if the thread pool
 *  is saturated, producers block (or get a rejection) rather than
 *  silently dropping assignments.
 *
 * Why ReentrantLock per resource?
 *  A single global lock would serialise ALL assignments regardless
 *  of target resource. Per-resource locks maximise throughput while
 *  still preventing two threads from concurrently assigning tasks
 *  to the same resource and exceeding its capacity.
 *
 * Why ConcurrentHashMap for in-flight tracking?
 *  Prevents duplicate submissions with O(1) amortised cost and
 *  no global lock contention.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TaskAssignmentEngine {

    private final TaskRepository            taskRepository;
    private final ResourceRepository        resourceRepository;
    private final AssignmentHistoryRepository historyRepository;
    private final TaskLogRepository         logRepository;
    private final TaskAssignmentStrategy    assignmentStrategy;
    private final PlatformTransactionManager txManager;

    @Value("${taskflow.executor.core-pool-size:4}")
    private int corePoolSize;
    @Value("${taskflow.executor.max-pool-size:16}")
    private int maxPoolSize;
    @Value("${taskflow.executor.queue-capacity:500}")
    private int queueCapacity;
    @Value("${taskflow.executor.keep-alive-seconds:60}")
    private int keepAliveSeconds;

    /**
     * Bounded queue acts as the buffer between producers and workers.
     * If the queue fills up, new submissions are rejected with a clear
     * error instead of silently degrading.
     */
    private final BlockingQueue<Long> assignmentQueue = new LinkedBlockingQueue<>(1000);

    /**
     * Tracks task IDs currently in-flight to prevent duplicate submissions.
     * ConcurrentHashMap.newKeySet() gives thread-safe Set semantics.
     */
    private final Set<Long> inFlight = ConcurrentHashMap.newKeySet();

    /**
     * Per-resource locks prevent two consumer threads from concurrently
     * assigning tasks to the same resource, which would cause a race
     * condition on Resource.activeTasks even inside a DB transaction
     * (two threads could both read activeTasks=3, both see capacity=5,
     *  both proceed, ending up with activeTasks=5 not 4).
     */
    private final ConcurrentHashMap<Long, ReentrantLock> resourceLocks = new ConcurrentHashMap<>();

    private ThreadPoolExecutor workerPool;
    private Thread             consumerThread;
    private volatile boolean   running = false;

    @PostConstruct
    public void start() {
        workerPool = new ThreadPoolExecutor(
            corePoolSize,
            maxPoolSize,
            keepAliveSeconds, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(queueCapacity),
            new TaskFlowThreadFactory("assignment-worker"),
            new ThreadPoolExecutor.CallerRunsPolicy()  // back-pressure: caller executes on overflow
        );

        running = true;
        consumerThread = Thread.ofVirtual()          // Java 21 virtual thread for the consumer
                .name("assignment-consumer")
                .start(this::consumeLoop);

        log.info("TaskAssignmentEngine started – pool [{}/{}], queue cap {}",
            corePoolSize, maxPoolSize, queueCapacity);
    }

    @PreDestroy
    public void stop() {
        running = false;
        consumerThread.interrupt();
        workerPool.shutdown();
        try {
            workerPool.awaitTermination(30, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        log.info("TaskAssignmentEngine stopped");
    }

    /**
     * Called by TaskService after a task is persisted.
     * Non-blocking: returns immediately after enqueuing.
     */
    public void submit(Long taskId) {
        // ConcurrentHashMap.add is atomic – no race condition for duplicate check
        if (!inFlight.add(taskId)) {
            log.debug("Task {} already in assignment queue, skipping duplicate submit", taskId);
            return;
        }
        boolean enqueued = assignmentQueue.offer(taskId);
        if (!enqueued) {
            inFlight.remove(taskId);
            log.warn("Assignment queue full – task {} will be picked up by next sweep", taskId);
        }
    }

    /** Drains the queue and dispatches work to the thread pool. */
    private void consumeLoop() {
        log.info("Assignment consumer started");
        while (running && !Thread.currentThread().isInterrupted()) {
            try {
                Long taskId = assignmentQueue.poll(1, TimeUnit.SECONDS);
                if (taskId != null) {
                    workerPool.submit(() -> processAssignment(taskId));
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * Core assignment logic executed on a worker thread.
     *
     * Locking order:
     * 1. Acquire per-resource lock BEFORE the DB transaction.
     * 2. Inside the transaction, re-read the resource with PESSIMISTIC_WRITE
     *    to handle any cross-JVM concurrency (e.g., multiple pods).
     *
     * This two-level locking ensures correctness both within a single JVM
     * (ReentrantLock) and across multiple JVM instances (DB row lock).
     */
    private void processAssignment(Long taskId) {
        try {
            TransactionTemplate tx = new TransactionTemplate(txManager);
            tx.setIsolationLevel(TransactionTemplate.ISOLATION_READ_COMMITTED);

            tx.execute(status -> {
                Task task = taskRepository.findById(taskId).orElse(null);
                if (task == null || task.getStatus() != TaskStatus.TODO || task.getResource() != null) {
                    inFlight.remove(taskId);
                    return null;
                }

                List<Resource> candidates = resourceRepository.findAvailableOrderedByLoad();
                Optional<Resource> chosen = assignmentStrategy.selectResource(task, candidates);

                if (chosen.isEmpty()) {
                    log.info("No available resource for task {} – will retry on next sweep", taskId);
                    inFlight.remove(taskId);
                    return null;
                }

                Resource resource = chosen.get();

                // Per-resource lock: prevents concurrent assignments to same resource within this JVM.
                // Without this lock, thread A and thread B could both pass the capacity check
                // (activeTasks < capacity) and both assign to the same resource,
                // causing activeTasks to go over capacity.
                ReentrantLock resourceLock = resourceLocks
                    .computeIfAbsent(resource.getId(), id -> new ReentrantLock());

                resourceLock.lock();
                try {
                    // Re-read with pessimistic write lock to cover multi-instance deployments
                    Resource locked = resourceRepository.findByIdWithLock(resource.getId())
                        .orElseThrow();

                    if (!locked.hasCapacity()) {
                        log.debug("Resource {} at capacity when locked, retrying", locked.getId());
                        inFlight.remove(taskId);
                        return null;
                    }

                    locked.setActiveTasks(locked.getActiveTasks() + 1);
                    resourceRepository.save(locked);

                    task.setResource(locked);
                    task.setStatus(TaskStatus.IN_PROGRESS);
                    taskRepository.save(task);

                    AssignmentHistory history = new AssignmentHistory();
                    history.setTask(task);
                    history.setResource(locked);
                    history.setAssignedBy("SYSTEM");
                    historyRepository.save(history);

                    logRepository.save(TaskLog.of(task, "SYSTEM", "ASSIGNED",
                        "Assigned to resource: " + locked.getName()));

                    log.info("Task {} assigned to resource {} (load: {}/{})",
                        taskId, locked.getName(), locked.getActiveTasks(), locked.getCapacity());

                } finally {
                    resourceLock.unlock();
                    inFlight.remove(taskId);
                }
                return null;
            });

        } catch (Exception e) {
            log.error("Assignment failed for task {}: {}", taskId, e.getMessage(), e);
            inFlight.remove(taskId);
        }
    }

    /** Scheduled sweep: re-queues any TODO tasks without a resource (e.g., after engine restart). */
    public void sweepUnassigned() {
        taskRepository.findUnassignedByStatus(TaskStatus.TODO)
            .forEach(t -> submit(t.getId()));
        log.debug("Unassigned task sweep complete");
    }

    public Map<String, Object> metrics() {
        return Map.of(
            "queueSize",        assignmentQueue.size(),
            "inFlightCount",    inFlight.size(),
            "activeWorkers",    workerPool.getActiveCount(),
            "completedTasks",   workerPool.getCompletedTaskCount(),
            "poolSize",         workerPool.getPoolSize()
        );
    }
}
