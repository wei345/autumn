package xyz.liuw.autumn.data;

import com.google.common.collect.Maps;
import com.vip.vjtools.vjkit.concurrent.ThreadUtil;
import com.vip.vjtools.vjkit.io.FileUtil;
import com.vip.vjtools.vjkit.io.IOUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.DigestUtils;
import org.springframework.util.StreamUtils;
import xyz.liuw.autumn.util.MimeTypeUtil;
import xyz.liuw.autumn.util.ResourceWalker;

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

import static java.nio.file.FileVisitResult.CONTINUE;

/**
 * @author liuwei
 * Created by liuwei on 2018/12/11.
 */
@Component
public class ResourceLoader {

    public static final String STATIC_ROOT = "/static";
    private static final String TEMPLATE_ROOT = "/templates";
    private static Logger logger = LoggerFactory.getLogger(ResourceLoader.class);
    private volatile long templateLastModified;
    @Value("${autumn.resource.reload-interval-seconds:10}")
    private long reloadIntervalSeconds;
    private volatile Map<String, ResourceLoader.ResourceCache> resourceCacheMap = Collections.emptyMap();
    private List<StaticChangedListener> staticChangedListeners = new ArrayList<>(1);
    private List<TemplateLastChangedListener> templateLastChangedListeners = new ArrayList<>(1);

    private volatile Thread timingReloadThread;

    @PostConstruct
    private void init() {
        refreshCache();
        timingRefreshCache();
    }

    @PreDestroy
    void stop() {
        if (timingReloadThread != null) {
            timingReloadThread.interrupt();
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

    public long getTemplateLastModified() {
        return templateLastModified;
    }

    private void timingRefreshCache() {
        if (isJar() || reloadIntervalSeconds <= 0) {
            return;
        }

        String threadName = getClass().getSimpleName() + ".timingRefreshCache";
        Thread thread = new Thread(() -> {
            logger.info("Started '{}' thread", threadName);
            while (!Thread.interrupted()) {
                ThreadUtil.sleep(reloadIntervalSeconds * 1000);
                refreshCache();
            }
            logger.info("Stopped thread '{}'", threadName);
        }, threadName);
        thread.setDaemon(true);
        thread.start();
        timingReloadThread = thread;
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
                String internalPath = path;
                if (path.startsWith(ResourceWalker.SPRING_BOOT_CLASSES)) {
                    internalPath = path.substring(ResourceWalker.SPRING_BOOT_CLASSES.length());
                }

                ResourceCache old = oldMap.get(internalPath);
                long lastModified = attrs.lastModifiedTime().toMillis();
                if (old != null && old.getLastModified() >= lastModified) {
                    pathToResourceCache.put(internalPath, old);
                    return CONTINUE;
                }

                addOrModifiedCount++;
                InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream("classpath:" + internalPath);
                byte[] content;
                try {
                    content = StreamUtils.copyToByteArray(in);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                } finally {
                    IOUtil.closeQuietly(in);
                }
                String md5 = DigestUtils.md5DigestAsHex(content);
                String mimeType = MimeTypeUtil.getMimeType(internalPath);
                ResourceCache resourceCache = new ResourceCache();
                resourceCache.setContent(content);
                resourceCache.setMd5(md5);
                resourceCache.setMimeType(mimeType);
                resourceCache.setLastModified(attrs.lastModifiedTime().toMillis());
                resourceCache.setPath(internalPath);
                pathToResourceCache.put(internalPath, resourceCache);
                return CONTINUE;
            }

            // file in file system
            String internalPath = classpathOfRoot + "/" + root.relativize(file).toString();
            ResourceCache old = oldMap.get(internalPath);
            long lastModified = attrs.lastModifiedTime().toMillis();
            if (old != null && old.getLastModified() >= lastModified) {
                pathToResourceCache.put(internalPath, old);
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
            resourceCache.setPath(internalPath);
            pathToResourceCache.put(internalPath, resourceCache);
            return CONTINUE;
        }

        Map<String, ResourceCache> getPathToResourceCache() {
            return pathToResourceCache;
        }

        boolean isChanged() {
            return addOrModifiedCount > 0 || pathToResourceCache.size() != oldMap.size();
        }
    }

    public static class ResourceCache {
        private byte[] content;
        private String md5; // content md5
        private String mimeType;
        private long lastModified; // file last modified
        private String path; // classpath

        public byte[] getContent() {
            return content;
        }

        public void setContent(byte[] content) {
            this.content = content;
        }

        public String getMd5() {
            return md5;
        }

        void setMd5(String md5) {
            this.md5 = md5;
        }

        public String getMimeType() {
            return mimeType;
        }

        void setMimeType(String mimeType) {
            this.mimeType = mimeType;
        }

        public long getLastModified() {
            return lastModified;
        }

        void setLastModified(long lastModified) {
            this.lastModified = lastModified;
        }

        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }
    }
}
