package xyz.liuw.autumn.service;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.freemarker.FreeMarkerProperties;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.File;
import java.net.URL;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * @author liuwei
 * Created by liuwei on 2018/11/19.
 */
@Component
public class TemplateWatcher {

    private static Logger logger = LoggerFactory.getLogger(TemplateWatcher.class);
    @Autowired
    private FreeMarkerProperties freeMarkerProperties;
    private String templateDir;
    private LoadingCache<String, Long> cache = CacheBuilder
            .newBuilder()
            .maximumSize(1000)
            .expireAfterWrite(10, TimeUnit.SECONDS)
            .build(new CacheLoader<String, Long>() {
                @Override
                public Long load(String key) {
                    return readTemplateLastModified(key);
                }
            });

    private volatile long fakeLastModified; // default 0

    @PostConstruct
    public void init() {
        setTemplateDir();
    }

    private void setTemplateDir() {
        String templateLoaderPath = freeMarkerProperties.getTemplateLoaderPath()[0];
        if (!templateLoaderPath.startsWith("classpath:")) {
            throw new RuntimeException("Unsupported non classpath: " + templateLoaderPath);
        }
        this.templateDir = templateLoaderPath.substring("classpath:".length());
        if (!templateDir.endsWith("/")) {
            this.templateDir += "/";
        }
        logger.info("templateDir: {}", templateLoaderPath);
    }

    /**
     * 返回模版最后修改时间。如果读取文件失败，则返回当前时间，随后的请求都返回这个时间不再访问文件。
     * 只有在开发时，模版文件才可能经常变化。返回固定的 "当前时间" 对线上没有影响。
     *
     * @param name 模版名，如 "main"
     * @return 模版最后修改时间
     */
    public long getTemplateLastModified(String name) {
        if (fakeLastModified > 0) {
            return fakeLastModified;
        }
        try {
            return cache.get(name);
        } catch (ExecutionException e) {
            logger.info("Failed to read template last modified, using fakeLastModified");
            synchronized (this) {
                if (fakeLastModified == 0) {
                    fakeLastModified = System.currentTimeMillis();
                }
            }
            return fakeLastModified;
        }
    }

    private long readTemplateLastModified(String name) {
        logger.debug("Reading template last modified '{}'", name);
        String path = templateDir + name + freeMarkerProperties.getSuffix();
        URL url = getClass().getResource(path);
        File file = new File(url.getPath());
        if (!file.exists()) {
            throw new RuntimeException("Template file not found " + url.getPath());
        }
        return file.lastModified();
    }


}
