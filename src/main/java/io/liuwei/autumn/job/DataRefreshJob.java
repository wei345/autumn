package io.liuwei.autumn.job;

import io.liuwei.autumn.config.AppProperties;
import io.liuwei.autumn.manager.MediaManager;
import io.liuwei.autumn.util.ScheduledJob;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.concurrent.ScheduledExecutorService;

/**
 * @author liuwei
 * @since 2021-07-11 17:42
 */
@Component
@RequiredArgsConstructor
public class DataRefreshJob {

    private final MediaManager mediaManager;

    private final AppProperties.SiteData data;

    private final ScheduledExecutorService scheduledExecutorService;

    @PostConstruct
    private void init() {
        new ScheduledJob(data.getReloadIntervalSeconds(), mediaManager::reload,
                scheduledExecutorService)
                .start();
    }
}
