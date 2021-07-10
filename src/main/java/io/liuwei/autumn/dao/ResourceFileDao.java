package io.liuwei.autumn.dao;

import com.google.common.collect.Maps;
import com.vip.vjtools.vjkit.concurrent.threadpool.ThreadPoolUtil;
import com.vip.vjtools.vjkit.io.FileUtil;
import com.vip.vjtools.vjkit.io.IOUtil;
import io.liuwei.autumn.util.MimeTypeUtil;
import io.liuwei.autumn.util.ResourceWalker;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.DigestUtils;
import org.springframework.util.StreamUtils;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.FileVisitResult.CONTINUE;

/**
 * @author liuwei
 * Created by liuwei on 2018/12/11.
 */
@Component
public class ResourceFileDao implements Runnable {

    public static final String STATIC_ROOT = "/static";
    private static final String WEBJARS_ROOT = "/META-INF/resources/webjars";
    private static final String TEMPLATE_ROOT = "/templates";
    private static final Logger logger = LoggerFactory.getLogger(ResourceFileDao.class);
    private final List<StaticChangedListener> staticChangedListeners = new ArrayList<>(1);
    private final List<TemplateLastChangedListener> templateLastChangedListeners = new ArrayList<>(1);
    private volatile long templateLastModified;
    @Value("${autumn.resource.reload-interval-seconds}")
    private long reloadIntervalSeconds;
    private volatile Map<String, ResourceFileDao.ResourceCache> resourceCacheMap = Collections.emptyMap();
    private ScheduledExecutorService scheduler;

    private static byte[] getResourceAsBytes(String classpath) {
        InputStream in = ResourceFileDao.class.getResourceAsStream(classpath);
        try {
            return StreamUtils.copyToByteArray(in);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            IOUtil.closeQuietly(in);
        }
    }

    @PostConstruct
    private void init() {
        refreshCache();
        startSchedule();
    }

    private void startSchedule() {
        if (isJar() || reloadIntervalSeconds <= 0) {
            return;
        }
        scheduler = Executors.newScheduledThreadPool(1,
                ThreadPoolUtil.buildThreadFactory("loadResource", true));
        schedule();
    }

    private void schedule() {
        if (!scheduler.isShutdown()) {
            scheduler.schedule(this, reloadIntervalSeconds, TimeUnit.SECONDS);
        }
    }

    @Override
    public void run() {
        refreshCache();
        schedule();
    }

    @PreDestroy
    void stop() {
        if (scheduler != null) {
            scheduler.shutdown();
        }
    }

    /**
     * @param path e.g. /static/js/script.js
     */
    public ResourceCache getResourceCache(String path) {
        return resourceCacheMap.get(path);
    }

    public void addTemplateLastChangedListener(TemplateLastChangedListener listener) {
        this.templateLastChangedListeners.add(listener);
    }

    public void addStaticChangedListener(StaticChangedListener listener) {
        this.staticChangedListeners.add(listener);
    }

    @SuppressWarnings("unused")
    public long getTemplateLastModified() {
        return templateLastModified;
    }

    private boolean isJar() {
        try {
            return getClass().getResource(STATIC_ROOT).toURI().getScheme().equalsIgnoreCase("jar");
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    private void refreshCache() {
        loadStaticDir();
        refreshTemplateLastModified();
    }

    private void refreshTemplateLastModified() {
        LastModifiedVisitor visitor = new LastModifiedVisitor();
        ResourceWalker.walk(TEMPLATE_ROOT, visitor);
        if (visitor.getLastModified() > templateLastModified) {
            templateLastModified = visitor.getLastModified();
            logger.info("Updated templateLastModified {}", templateLastModified);
            templateLastChangedListeners.forEach(TemplateLastChangedListener::onChanged);
        }
    }

    @SuppressWarnings("SameParameterValue")
    private void loadStaticDir() {
        String classpathOfRoot = STATIC_ROOT;
        LoadResourceVisitor visitor = new LoadResourceVisitor(classpathOfRoot, resourceCacheMap);
        ResourceWalker.walk(classpathOfRoot, visitor);
        if (visitor.isChanged()) {
            resourceCacheMap = visitor.getPathToResourceCache();
            staticChangedListeners.forEach(StaticChangedListener::onChanged);
        }
    }

    public String getWebJarResourceAsString(String internalPath) {
        return new String(getResourceAsBytes(WEBJARS_ROOT + internalPath), UTF_8);
    }

    public interface TemplateLastChangedListener {
        void onChanged();
    }

    public interface StaticChangedListener {
        void onChanged();
    }

    static class LastModifiedVisitor extends ResourceWalker.SkipHiddenFileVisitor {
        private long lastModified;

        @Override
        public FileVisitResult visitNonHiddenFile(Path file, BasicFileAttributes attrs) {
            long time = attrs.lastModifiedTime().toMillis();
            if (time > lastModified) {
                lastModified = time;
            }
            return CONTINUE;
        }

        long getLastModified() {
            return lastModified;
        }
    }

    static class LoadResourceVisitor extends SimpleFileVisitor<Path> {
        private Map<String, ResourceCache> oldMap;
        private Map<String, ResourceCache> pathToResourceCache;
        private Path root;
        private int addOrModifiedCount;
        private String classpathOfRoot; // e.g. /static

        LoadResourceVisitor(String classpathOfRoot, Map<String, ResourceCache> oldMap) {
            Validate.isTrue(classpathOfRoot.startsWith("/"));
            this.classpathOfRoot = (classpathOfRoot.length() == 1) ? "" : classpathOfRoot;
            this.oldMap = oldMap;
            this.pathToResourceCache = Maps.newHashMapWithExpectedSize(oldMap.size());
        }

        @Override
        public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
            if (root == null) {
                root = dir;
            }
            if (ResourceWalker.isHidden(dir)) {
                return FileVisitResult.SKIP_SUBTREE;
            }
            return super.preVisitDirectory(dir, attrs);
        }

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
            if (ResourceWalker.isHidden(file)) {
                return CONTINUE;
            }

            String path = file.toString();

            // file in jar
            if (StringUtils.containsIgnoreCase(file.getFileSystem().getClass().getSimpleName(), "zip")) {
                String fileClasspath = path;
                if (path.startsWith(ResourceWalker.SPRING_BOOT_CLASSES)) {
                    fileClasspath = path.substring(ResourceWalker.SPRING_BOOT_CLASSES.length());
                }

                ResourceCache old = oldMap.get(fileClasspath);
                long lastModified = attrs.lastModifiedTime().toMillis();
                if (old != null && old.getLastModified() >= lastModified) {
                    pathToResourceCache.put(fileClasspath, old);
                    return CONTINUE;
                }

                addOrModifiedCount++;

                byte[] content = getResourceAsBytes(fileClasspath);
                String md5 = DigestUtils.md5DigestAsHex(content);
                String mimeType = MimeTypeUtil.getMimeType(fileClasspath);
                ResourceCache resourceCache = new ResourceCache();
                resourceCache.setContent(content);
                resourceCache.setMd5(md5);
                resourceCache.setMimeType(mimeType);
                resourceCache.setLastModified(attrs.lastModifiedTime().toMillis());
                resourceCache.setPath(fileClasspath);
                pathToResourceCache.put(fileClasspath, resourceCache);
                return CONTINUE;
            }

            // file in file system
            String fileClasspath = classpathOfRoot + "/" + root.relativize(file).toString();
            ResourceCache old = oldMap.get(fileClasspath);
            long lastModified = attrs.lastModifiedTime().toMillis();
            if (old != null && old.getLastModified() >= lastModified) {
                pathToResourceCache.put(fileClasspath, old);
                return CONTINUE;
            }

            addOrModifiedCount++;
            File f = new File(path);
            byte[] content;
            try {
                content = FileUtil.toByteArray(f);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            String md5 = DigestUtils.md5DigestAsHex(content);
            String mimeType = MimeTypeUtil.getMimeType(f.getName());
            ResourceCache resourceCache = new ResourceCache();
            resourceCache.setContent(content);
            resourceCache.setMd5(md5);
            resourceCache.setMimeType(mimeType);
            resourceCache.setLastModified(lastModified);
            resourceCache.setPath(fileClasspath);
            pathToResourceCache.put(fileClasspath, resourceCache);
            return CONTINUE;
        }

        Map<String, ResourceCache> getPathToResourceCache() {
            return pathToResourceCache;
        }

        boolean isChanged() {
            return addOrModifiedCount > 0 || pathToResourceCache.size() != oldMap.size();
        }
    }

    @Getter
    @Setter
    public static class ResourceCache {
        private byte[] content;
        private String md5; // content md5
        private String mimeType;
        private long lastModified; // file last modified
        private String path; // classpath

        public String getContentString() {
            return new String(content, UTF_8);
        }

    }
}
