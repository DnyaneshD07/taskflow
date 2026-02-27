package com.taskflow.concurrency;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Names threads for easy identification in thread-dump analysis.
 */
public class TaskFlowThreadFactory implements ThreadFactory {

    private final String prefix;
    private final AtomicInteger counter = new AtomicInteger(1);

    public TaskFlowThreadFactory(String prefix) {
        this.prefix = prefix;
    }

    @Override
    public Thread newThread(Runnable r) {
        Thread t = new Thread(r, prefix + "-" + counter.getAndIncrement());
        t.setDaemon(true);
        return t;
    }
}
