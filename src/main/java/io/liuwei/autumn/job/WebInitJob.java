package io.liuwei.autumn.job;

import io.liuwei.autumn.util.HttpURLConnectionUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 在我的服务器上 JS 压缩可能会花 4s 时间，提前触发一下。
 *
 * @author liuwei
 * @since 2021-07-18 16:34
 */
@Component
@Slf4j
public class WebInitJob {

    @Autowired
    private ServerProperties serverProperties;

    @EventListener(ApplicationReadyEvent.class)
    public void onReady() {
        new Thread(() -> {
            try {
                getHomepage();
            } catch (Throwable t) {
                log.error("", t);
            }
        }, getClass().getSimpleName()).start();
    }

    /**
     * 访问首页可以触发构建 all.js 和 all.css
     */
    private void getHomepage() {
        String url = String.format("http://localhost:%s%s",
                serverProperties.getPort(), serverProperties.getServlet().getContextPath());
        log.info("Initializing Homepage, url: {}", url);
        long start = System.currentTimeMillis();
        HttpURLConnectionUtil.Response response = HttpURLConnectionUtil
                .request("get", url, null, null);
        if (response.isOk()) {
            log.info("Homepage OK. {}ms", System.currentTimeMillis() - start);
        } else {
            log.warn("Homepage ERROR. {}", response);
        }
    }

}
