package io.liuwei.autumn.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.common.io.BaseEncoding;
import com.vip.vjtools.vjkit.concurrent.threadpool.ThreadPoolUtil;
import com.vip.vjtools.vjkit.mapper.JsonMapper;
import io.liuwei.autumn.aop.SettingModelAttributeInterceptor;
import io.liuwei.autumn.constant.CacheNames;
import io.liuwei.autumn.service.UserService;
import io.liuwei.autumn.component.AsciidocArticleParser;
import org.asciidoctor.Asciidoctor;
import org.asciidoctor.jruby.internal.JRubyAsciidoctor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.Cache;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * @author liuwei
 * Created by liuwei on 2018/11/23.
 */
@Configuration
@EnableConfigurationProperties(AppProperties.class)
public class AppConfig {

    @Value("${server.servlet.context-path:}")
    private String contextPath;

    /**
     * cookie name 和 localStorage key 前缀，用于区分域名相同 contextPath 不同的 Autumn 实例。
     */
    private String prefix;

    @PostConstruct
    private void init() {
        setPrefix();
    }

    private void setPrefix() {
        String prefix = "autumn.";
        // 可以把域名相同 port 不同或 contentPath 不同的 Autumn 实例看作不同实例。
        // port 不是配置文件里的 server.port，是 request url 里的 port，
        // 也就是 Host header 里的 port，如果有反向代理，需要把 Host 传给 Autumn。
        // 每次计算 instance 需要从 request 中读取 port，把 request 一路传过来有点麻烦。
        // 可以简单省事直接在配置文件里加一条配置，指定最终用户访问的 port。
        // 但我不需要区分 port，所以这里只考虑 contextPath。
        // 我设置 Autumn contextPath 和最终用户（经反向代理）访问的 contextPath 是相同的。
        if (contextPath.length() > 0) {
            prefix = prefix + BaseEncoding.base64Url().omitPadding().encode(contextPath.getBytes(UTF_8)) + ".";
        }
        this.prefix = prefix;
    }

    @Bean
    public UserService userService() {
        return new UserService(prefix + "me");
    }

    @Bean
    public SettingModelAttributeInterceptor settingModelAttributeInterceptor() {
        return new SettingModelAttributeInterceptor(prefix);
    }

    @Bean
    public JsonMapper jsonMapper() {
        return JsonMapper.nonNullMapper();
    }

    @Bean
    public Asciidoctor asciidoctor() {
        return new JRubyAsciidoctor();
    }

    @Bean
    public AsciidocArticleParser asciidocArticleParser(Asciidoctor asciidoctor) {
        return new AsciidocArticleParser(asciidoctor);
    }

    @Bean
    public ScheduledExecutorService scheduledExecutorService() {
        return Executors.newScheduledThreadPool(
                1,
                ThreadPoolUtil.buildThreadFactory("Scheduled-Load-File", true));
    }

    @Bean
    public Caffeine<?, ?> caffeine() {
        return Caffeine
                .newBuilder()
                .maximumSize(10_000);
    }

    @Bean
    public Cache hitCache() {
        return new CaffeineCache(
                CacheNames.ARTICLE_HIT,
                Caffeine
                        .newBuilder()
                        .maximumSize(50_000) // 约每篇文章缓存 100 个词的匹配结果
                        .build());
    }

    @Bean
    public Cache mediaCache() {
        return new CaffeineCache(
                CacheNames.MEDIA_CONTENT,
                Caffeine
                        .newBuilder()
                        .maximumSize(1000)
                        .build());
    }

    @Bean
    public Cache viewCache() {
        return new CaffeineCache(
                CacheNames.VIEW_HTML,
                Caffeine
                        .newBuilder()
                        .maximumSize(10_000)
                        .build());
    }

}
