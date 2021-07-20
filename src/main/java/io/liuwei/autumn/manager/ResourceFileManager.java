package io.liuwei.autumn.manager;

import com.google.common.collect.Maps;
import io.liuwei.autumn.constant.CacheNames;
import io.liuwei.autumn.constant.Constants;
import io.liuwei.autumn.model.ResourceFile;
import io.liuwei.autumn.util.IOUtil;
import io.liuwei.autumn.util.ResourceWalker;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.Cache;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collections;
import java.util.Map;

import static java.nio.file.FileVisitResult.CONTINUE;

/**
 * @author liuwei
 * Created by liuwei on 2018/12/11.
 */
@SuppressWarnings("FieldCanBeLocal")
@Component
@Slf4j
public class ResourceFileManager {

    @Autowired
    @Qualifier("viewCache")
    private Cache viewCache;

    @Autowired
    private ResourceFileManager aopProxy;

    @Getter
    @Value("${autumn.static.dir}")
    private String staticRoot;

    @Value("${spring.thymeleaf.prefix}")
    private String templateRoot;

    private volatile Map<String, ResourceFile> resourceFileMap = Collections.emptyMap();

    private volatile long templateLastModified;

    @PostConstruct
    private void init() {
        refreshCache();
    }

    public void refreshCache() {
        refreshStatic();
        refreshTemplateLastModified();
    }

    @SuppressWarnings("SameParameterValue")
    private void refreshStatic() {
        String staticRoot = this.staticRoot;
        LoadResourceFileVisitor visitor = new LoadResourceFileVisitor(staticRoot, resourceFileMap);
        ResourceWalker.walk(staticRoot, visitor);
        if (visitor.isChanged()) {
            resourceFileMap = visitor.getResourceFileMap();
            aopProxy.clearStaticCache();
            viewCache.clear();
            log.info("static changed. {}", staticRoot);
        }
    }

    @CacheEvict(value = CacheNames.STATIC, allEntries = true)
    public void clearStaticCache() {
    }

    private void refreshTemplateLastModified() {
        GetLastModifiedVisitor visitor = new GetLastModifiedVisitor();
        ResourceWalker.walk(templateRoot, visitor);
        if (visitor.getLastModified() > templateLastModified) {
            templateLastModified = visitor.getLastModified();
            viewCache.clear();
            log.info("template changed. {}", templateRoot);
        }
    }

    /**
     * @param path e.g. /static/js/script.js
     */
    public ResourceFile getResourceFile(String path) {
        return resourceFileMap.get(path);
    }

    private static boolean isHidden(Path file) {
        return file.getFileName().toString().startsWith(".");
    }

    static class GetLastModifiedVisitor extends SimpleFileVisitor<Path> {
        @Getter
        private long lastModified;

        @Override
        public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
            if (isHidden(dir)) {
                return FileVisitResult.SKIP_SUBTREE;
            }
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
            if (isHidden(file)) {
                return CONTINUE;
            }

            long time = attrs.lastModifiedTime().toMillis();
            if (time > lastModified) {
                lastModified = time;
            }
            return CONTINUE;
        }
    }

    static class LoadResourceFileVisitor extends SimpleFileVisitor<Path> {
        private final Map<String, ResourceFile> oldMap;
        @Getter
        private final Map<String, ResourceFile> resourceFileMap;
        private final String rootPath; // e.g. /static
        private Path root;
        private int addOrModifiedCount;

        LoadResourceFileVisitor(String rootPath, Map<String, ResourceFile> oldMap) {
            this.rootPath = rootPath;
            this.oldMap = oldMap;
            this.resourceFileMap = Maps.newHashMapWithExpectedSize(oldMap.size());
        }

        @Override
        public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
            if (root == null) {
                root = dir;
            }
            if (isHidden(dir)) {
                return FileVisitResult.SKIP_SUBTREE;
            }
            return super.preVisitDirectory(dir, attrs);
        }

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
            if (isHidden(file)) {
                return CONTINUE;
            }

            String path = file.toString();

            if (StringUtils.containsIgnoreCase(file.getFileSystem().getClass().getSimpleName(), "zip")) {
                // file in jar
                String filePath = path;
                if (path.startsWith(Constants.BOOT_INF_CLASSES)) {
                    filePath = path.substring(Constants.BOOT_INF_CLASSES.length());
                }
                filePath = ResourceWalker.CLASSPATH_PREFIX + filePath;

                ResourceFile old = oldMap.get(filePath);
                if (old != null && old.getLastModified() >= attrs.lastModifiedTime().toMillis()) {
                    resourceFileMap.put(filePath, old);
                    return CONTINUE;
                }

                addOrModifiedCount++;

                byte[] content = IOUtil.resourceToByteArray(filePath);
                ResourceFile resourceFile = new ResourceFile();
                resourceFile.setContent(content);
                resourceFile.setLastModified(attrs.lastModifiedTime().toMillis());
                resourceFileMap.put(filePath, resourceFile);
                return CONTINUE;
            } else {
                // file in file system
                String filePath = rootPath + "/" + root.relativize(file).toString();
                ResourceFile old = oldMap.get(filePath);
                long lastModified = attrs.lastModifiedTime().toMillis();
                if (old != null && old.getLastModified() >= lastModified) {
                    resourceFileMap.put(filePath, old);
                    return CONTINUE;
                }

                addOrModifiedCount++;
                byte[] content = IOUtil.toByteArray(file);
                ResourceFile resourceFile = new ResourceFile();
                resourceFile.setContent(content);
                resourceFile.setLastModified(lastModified);
                resourceFileMap.put(filePath, resourceFile);
                return CONTINUE;
            }
        }

        boolean isChanged() {
            return addOrModifiedCount > 0 || resourceFileMap.size() != oldMap.size();
        }
    }

}
