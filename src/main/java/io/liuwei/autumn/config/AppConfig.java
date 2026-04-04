package io.liuwei.autumn.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.common.io.BaseEncoding;
import com.vip.vjtools.vjkit.concurrent.threadpool.ThreadPoolUtil;
import io.liuwei.autumn.aop.CommonModelAttributeInterceptor;
import io.liuwei.autumn.parser.AsciidocArticleParser;
import io.liuwei.autumn.constant.CacheNames;
import io.liuwei.autumn.service.UserService;
import io.liuwei.autumn.util.JsonMapper;
import org.asciidoctor.Asciidoctor;
import org.asciidoctor.jruby.internal.JRubyAsciidoctor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.Cache;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import static java.nio.charset.StandardCharsets.*;

/**
 * @author liuwei
 * Created by liuwei on 2018/11/23.
 */
@Configuration
@EnableCaching
@EnableConfigurationProperties(AppProperties.class)
public class AppConfig {

    /**
     * cookie name 和 localStorage key 前缀，用于区分域名相同 contextPath 不同的 Autumn 实例。
     */
    private final String prefix;

    private final AppProperties appProperties;

    public AppConfig(@Value("${server.servlet.context-path:}") String contextPath,
                     AppProperties appProperties) {
        this.prefix = buildPrefix(contextPath);
        this.appProperties = appProperties;
    }

    private String buildPrefix(String contextPath) {
        String prefix = "autumn.";
        // 可以把域名相同 port 不同或 contentPath 不同的 Autumn 实例看作不同实例。
        // port 不是配置文件里的 server.port，是 request url 里的 port，
        // 也就是 Host header 里的 port，如果有反向代理，需要把 Host 传给 Autumn。
        // 每次计算 instance 需要从 request 中读取 port，把 request 一路传过来有点麻烦。
        // 可以简单省事直接在配置文件里加一条配置，指定最终用户访问的 port。
        // 但我不需要区分 port，所以这里只考虑 contextPath。
        // 我设置 Autumn contextPath 和最终用户（经反向代理）访问的 contextPath 是相同的。
        if (contextPath.length() > 0) {
            return prefix + BaseEncoding.base64Url().omitPadding().encode(contextPath.getBytes(UTF_8)) + ".";
        } else {
            return prefix;
        }
    }

    @Bean
    public UserService userService(AppProperties.Access access, AppProperties.RememberMe rememberMe) {
        return new UserService(prefix + "me", access, rememberMe);
    }

    @Bean
    public CommonModelAttributeInterceptor settingModelAttributeInterceptor() {
        return new CommonModelAttributeInterceptor(prefix);
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
    public ScheduledExecutorService scheduledExecutorService() {
        return Executors.newScheduledThreadPool(
                1,
                ThreadPoolUtil.buildThreadFactory("Scheduled-Load-File", true));
    }

    @Bean
    public Caffeine<?, ?> caffeine() {
        return Caffeine
                .newBuilder()
                .maximumSize(appProperties.getCache().getDefaultEntries());
    }

    @Bean
    public Cache hitCache() {
        return new CaffeineCache(
                CacheNames.ARTICLE_HIT,
                Caffeine
                        .newBuilder()
                        .maximumSize(appProperties.getCache().getMaxSearchHitArticleEntries())
                        .build());
    }

    @Bean
    public Cache mediaCache() {
        return new CaffeineCache(
                CacheNames.MEDIA_CONTENT,
                Caffeine
                        .newBuilder()
                        .maximumSize(appProperties.getCache().getMaxMediaEntries())
                        .build());
    }

    @Bean
    public Cache viewCache() {
        return new CaffeineCache(
                CacheNames.VIEW_HTML,
                Caffeine
                        .newBuilder()
                        .maximumSize(appProperties.getCache().getMaxHtmlViewEntries())
                        .build());
    }

    @Bean
    public AppProperties.Access appAccess() {
        return appProperties.getAccess();
    }

    @Bean
    public AppProperties.RememberMe appRememberMe() {
        return appProperties.getRememberMe();
    }

    @Bean
    public AppProperties.SiteData appData() {
        return appProperties.getData();
    }

    @Bean
    public AppProperties.StaticResource appStaticResource() {
        return appProperties.getStaticResource();
    }

    @Bean
    public AppProperties.Cache appCache() {
        return appProperties.getCache();
    }

    @Bean
    public AppProperties.CodeBlock appCodeBlock() {
        return appProperties.getCodeBlock();
    }

    @Bean
    public AppProperties.Breadcrumb appBreadcrumb() {
        return appProperties.getBreadcrumb();
    }

    @Bean
    public AppProperties.Search appSearch() {
        return appProperties.getSearch();
    }

    @Bean
    public AppProperties.Analytics appAnalytics() {
        return appProperties.getAnalytics();
    }

}
