package io.liuwei.autumn.manager;

import com.google.common.collect.Maps;
import io.liuwei.autumn.constant.CacheConstants;
import io.liuwei.autumn.constant.Constants;
import io.liuwei.autumn.model.ResourceFile;
import io.liuwei.autumn.util.IOUtil;
import io.liuwei.autumn.util.MimeTypeUtil;
import io.liuwei.autumn.util.ResourceWalker;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Component;
import org.springframework.util.DigestUtils;

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
    private ResourceFileManager proxy;

    @Getter
    private String staticRoot = "/static";

    private String templateRoot = "/templates";

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
            proxy.clearStaticCache();
        }
    }

    @CacheEvict(value = CacheConstants.STATIC, allEntries = true)
    public void clearStaticCache() {
        log.info("clearStaticCache");
    }

    private void refreshTemplateLastModified() {
        GetLastModifiedVisitor visitor = new GetLastModifiedVisitor();
        ResourceWalker.walk(templateRoot, visitor);
        if (visitor.getLastModified() > templateLastModified) {
            templateLastModified = visitor.getLastModified();
            log.info("Updated templateLastModified {}", templateLastModified);
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
        private Map<String, ResourceFile> oldMap;
        @Getter
        private Map<String, ResourceFile> resourceFileMap;
        private Path root;
        private int addOrModifiedCount;
        private String classpathOfRoot; // e.g. /static

        LoadResourceFileVisitor(String classpathOfRoot, Map<String, ResourceFile> oldMap) {
            Validate.isTrue(classpathOfRoot.startsWith("/"));
            this.classpathOfRoot = (classpathOfRoot.length() == 1) ? "" : classpathOfRoot;
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
                String fileClasspath = path;
                if (path.startsWith(Constants.BOOT_INF_CLASSES)) {
                    fileClasspath = path.substring(Constants.BOOT_INF_CLASSES.length());
                }

                ResourceFile old = oldMap.get(fileClasspath);
                if (old != null && old.getLastModified() >= attrs.lastModifiedTime().toMillis()) {
                    resourceFileMap.put(fileClasspath, old);
                    return CONTINUE;
                }

                addOrModifiedCount++;

                byte[] content = IOUtil.resourceToByteArray(fileClasspath);
                String md5 = DigestUtils.md5DigestAsHex(content);
                String mimeType = MimeTypeUtil.getMimeType(fileClasspath);
                ResourceFile resourceFile = new ResourceFile();
                resourceFile.setContent(content);
                resourceFile.setMd5(md5);
                resourceFile.setMimeType(mimeType);
                resourceFile.setLastModified(attrs.lastModifiedTime().toMillis());
                resourceFile.setPath(fileClasspath);
                resourceFileMap.put(fileClasspath, resourceFile);
                return CONTINUE;
            } else {
                // file in file system
                String fileClasspath = classpathOfRoot + "/" + root.relativize(file).toString();
                ResourceFile old = oldMap.get(fileClasspath);
                long lastModified = attrs.lastModifiedTime().toMillis();
                if (old != null && old.getLastModified() >= lastModified) {
                    resourceFileMap.put(fileClasspath, old);
                    return CONTINUE;
                }

                addOrModifiedCount++;
                byte[] content = IOUtil.toByteArray(file);
                String md5 = DigestUtils.md5DigestAsHex(content);
                String mimeType = MimeTypeUtil.getMimeType(file.toFile().getName());
                ResourceFile resourceFile = new ResourceFile();
                resourceFile.setContent(content);
                resourceFile.setMd5(md5);
                resourceFile.setMimeType(mimeType);
                resourceFile.setLastModified(lastModified);
                resourceFile.setPath(fileClasspath);
                resourceFileMap.put(fileClasspath, resourceFile);
                return CONTINUE;
            }
        }

        boolean isChanged() {
            return addOrModifiedCount > 0 || resourceFileMap.size() != oldMap.size();
        }
    }

}
