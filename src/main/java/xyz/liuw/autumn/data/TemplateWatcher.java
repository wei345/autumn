package xyz.liuw.autumn.data;

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

    @PostConstruct
    public void init() {
        setTemplateDir();
    }

    public long getTemplateLastModified(String name) {
        try {
            return cache.get(name);
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    private long readTemplateLastModified(String name) {
        logger.debug("reading template '{}' last modified", name);
        String path = templateDir + name + freeMarkerProperties.getSuffix();
        URL url = getClass().getResource(path);
        File file = new File(url.getPath());
        if (!file.exists()) {
            throw new RuntimeException("template not found" + url.getPath());
        }
        return file.lastModified();
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


}
