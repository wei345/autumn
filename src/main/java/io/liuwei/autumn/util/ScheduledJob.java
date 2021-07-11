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

    private Runnable command;

    private ScheduledExecutorService scheduler;

    public ScheduledJob(int intervalSeconds, Runnable command, ScheduledExecutorService scheduler) {
        if (command == null) {
            throw new NullPointerException("command");
        }

        this.intervalSeconds = intervalSeconds;

        this.command = command;

        this.scheduler = scheduler;
    }

    public void start() {
        if (intervalSeconds > 0) {
            schedule();
            log.info("scheduled. intervalSeconds={}, command={}", intervalSeconds, command);
        } else {
            log.info("will not run. intervalSeconds={}, command={}", intervalSeconds, command);
        }
    }

    private void schedule() {
        if (!scheduler.isShutdown()) {
            scheduler.schedule(
                    () -> {
                        try {
                            command.run();
                            schedule();
                        } catch (Throwable t) {
                            log.error("调度任务执行失败，调度停止. command=" + command, t);
                        }
                    },
                    intervalSeconds,
                    TimeUnit.SECONDS);
        }
    }
}
