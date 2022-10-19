package io.liuwei.autumn.job;

import io.liuwei.autumn.config.AppProperties;
import io.liuwei.autumn.manager.ResourceFileManager;
import io.liuwei.autumn.util.ScheduledJob;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.concurrent.ScheduledExecutorService;

/**
 * @author liuwei
 * @since 2021-07-11 16:54
 */
@Component
@RequiredArgsConstructor
public class ResourceFileRefreshJob {

    private final ResourceFileManager resourceFileManager;

    private final AppProperties.StaticResource staticResource;

    @Autowired
    private ScheduledExecutorService scheduledExecutorService;

    @PostConstruct
    private void init() {
        new ScheduledJob(staticResource.getReloadIntervalSeconds(),
                resourceFileManager::refreshCache, scheduledExecutorService)
                .start();
    }

}
