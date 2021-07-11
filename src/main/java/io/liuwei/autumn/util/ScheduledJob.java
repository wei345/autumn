package io.liuwei.autumn.util;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * @author liuwei
 * @since 2021-07-11 17:20
 */
@Slf4j
public class ScheduledJob {

    private int intervalSeconds;

    private Task task;

    private ScheduledExecutorService scheduler;

    public ScheduledJob(int intervalSeconds, Runnable command, ScheduledExecutorService scheduler) {
        if (intervalSeconds <= 0) {
            throw new IllegalArgumentException("intervalSeconds");
        }

        if (command == null) {
            throw new NullPointerException("command");
        }

        this.scheduler = scheduler;

        this.intervalSeconds = intervalSeconds;

        this.task = new Task(command);
    }

    public void start() {
        schedule();
    }

    private void schedule() {
        if (!scheduler.isShutdown()) {
            scheduler.schedule(task, intervalSeconds, TimeUnit.SECONDS);
        }
    }

    private class Task implements Runnable {

        private Runnable command;

        public Task(Runnable command) {
            this.command = command;
        }

        @Override
        public void run() {
            try {
                command.run();
                schedule();
            } catch (Throwable t) {
                log.error("调度任务执行失败，调度停止", t);
            }
        }
    }
}
