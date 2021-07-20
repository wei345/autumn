package io.liuwei.autumn.job;

import io.liuwei.autumn.manager.MediaManager;
import io.liuwei.autumn.util.ScheduledJob;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.concurrent.ScheduledExecutorService;

/**
 * @author liuwei
 * @since 2021-07-11 17:42
 */
@Component
public class DataRefreshJob {

    @Autowired
    private MediaManager mediaManager;

    @Value("${autumn.data.reload-interval-seconds}")
    private int reloadIntervalSeconds;

    @Autowired
    private ScheduledExecutorService scheduledExecutorService;

    @PostConstruct
    private void init() {
        new ScheduledJob(reloadIntervalSeconds, mediaManager::reload, scheduledExecutorService)
                .start();
    }
}
