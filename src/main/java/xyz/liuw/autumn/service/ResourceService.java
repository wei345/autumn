package xyz.liuw.autumn.service;

import com.google.common.collect.Maps;
import com.vip.vjtools.vjkit.io.FileUtil;
import com.vip.vjtools.vjkit.io.IOUtil;
import com.vip.vjtools.vjkit.text.StringBuilderHolder;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.DigestUtils;
import org.springframework.util.StreamUtils;
import xyz.liuw.autumn.data.TreeJson;
import xyz.liuw.autumn.util.ResourceWalker;
import xyz.liuw.autumn.util.WebUtil;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collections;
import java.util.Map;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.FileVisitResult.CONTINUE;

/**
 * @author liuwei
 * Created by liuwei on 2018/11/19.
 */
@Component
public class ResourceService {

    private static final String STATIC_ROOT = "/static";
    private static final String TEMPLATE_ROOT = "/templates";
    private static Logger logger = LoggerFactory.getLogger(ResourceService.class);
    @Autowired
    private DataService dataService;

    private volatile Map<String, ResourceCache> resourceCacheMap = Collections.emptyMap();

    private volatile JsCache jsCache;

    private volatile CssCache cssCache;

    private volatile long templateLastModified;

    @Value("${autumn.resource.reload-interval-seconds:10}")
    private long reloadIntervalSeconds;

    @Autowired
    private WebUtil webUtil;

    public long getTemplateLastModified() {
        return templateLastModified;
    }

    public JsCache getJsCache() {
        return jsCache;
    }

    public CssCache getCssCache() {
        return cssCache;
    }

    @PostConstruct
    private void init() {
        refreshCache();
        timingRefreshCache();
    }

    private void timingRefreshCache() {
        if (isJar()) {
            logger.info("static 和 templates 在 jar 中，不启动定时刷新");
            return;
        }

        String threadName = getClass().getSimpleName() + ".timingRefreshCache";
        Thread thread = new Thread(() -> {
            logger.info("Started '{}' thread", threadName);
            while (reloadIntervalSeconds > 0) {
                try {
                    long t = reloadIntervalSeconds * 1000;
                    Thread.sleep(t);
                } catch (InterruptedException ignore) {
                }
                refreshCache();
            }
        }, threadName);
        thread.setDaemon(true);
        thread.start();
    }

    private boolean isJar() {
        try {
            return getClass().getResource(STATIC_ROOT).toURI().getScheme().equalsIgnoreCase("jar");
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    private void refreshCache() {
        //noinspection NonAtomicOperationOnVolatileField
        resourceCacheMap = loadClassPathResourceDir(STATIC_ROOT, resourceCacheMap);
        refreshJsCache();
        refreshCssCache();
        refreshTemplateLastModified();
    }

    private void refreshTemplateLastModified() {
        LastModifiedVisitor visitor = new LastModifiedVisitor();
        ResourceWalker.walk(TEMPLATE_ROOT, visitor);
        if (visitor.getLastModified() > this.templateLastModified) {
            this.templateLastModified = visitor.getLastModified();
            logger.info("templateLastModified updated");
        }
    }


    private void refreshJsCache() {
        ResourceCache scriptJs = resourceCacheMap.get(STATIC_ROOT + "/js/script.js");
        TreeJson treeJson = dataService.getTreeJson();
        if (jsCache != null && jsCache.checkNotModified(treeJson.getMd5(), scriptJs.getMd5())) {
            return;
        }

        StringBuilder sb = StringBuilderHolder.getGlobal();
        sb.append("window.autumn = {ctx: '")
                .append(webUtil.getContextPath())
                .append("'}; ")
                .append(new String(scriptJs.getContent(), UTF_8));

        String jsText = sb.toString();
        JsCache jsCache = new JsCache();
        jsCache.setContent(jsText.getBytes(UTF_8));
        String md5 = DigestUtils.md5DigestAsHex(jsCache.getContent());
        String etag = WebUtil.padEtagIfNecessary(md5);
        String version = md5.substring(0, 7);
        jsCache.setEtag(etag);
        jsCache.setVersion(version);
        jsCache.setScriptJsMd5(scriptJs.getMd5());
        jsCache.setTreeJsonMd5(treeJson.getMd5());
        this.jsCache = jsCache;
        this.templateLastModified = System.currentTimeMillis();
        logger.info("jsCache updated");
    }

    private void refreshCssCache() {
        ResourceCache normalizeCss = resourceCacheMap.get(STATIC_ROOT + "/css/normalize.css");
        ResourceCache styleCss = resourceCacheMap.get(STATIC_ROOT + "/css/style.css");
        if (cssCache != null && cssCache.checkNotModified(normalizeCss.getMd5(), styleCss.getMd5())) {
            return;
        }

        String cssText = new String(normalizeCss.getContent(), UTF_8) + new String(styleCss.getContent(), UTF_8);
        CssCache cssCache = new CssCache();
        cssCache.setContent(cssText.getBytes(UTF_8));
        String md5 = DigestUtils.md5DigestAsHex(cssCache.getContent());
        String etag = WebUtil.padEtagIfNecessary(md5);
        String version = md5.substring(0, 7);
        cssCache.setEtag(etag);
        cssCache.setVersion(version);
        cssCache.setNormalizeCssMd5(normalizeCss.getMd5());
        cssCache.setStyleCssMd5(styleCss.getMd5());
        this.cssCache = cssCache;
        this.templateLastModified = System.currentTimeMillis();
        logger.info("cssCache updated");
    }


    /**
     * @param rootDir classpath 里的目录，加载该目录下的所有文件。e.g. /static
     */
    @SuppressWarnings("SameParameterValue")
    private Map<String, ResourceCache> loadClassPathResourceDir(String rootDir, Map<String, ResourceCache> oldMap) {
        LoadResourceVisitor visitor = new LoadResourceVisitor(oldMap);
        ResourceWalker.walk(rootDir, visitor);
        return visitor.getMap();
    }

    static class LoadResourceVisitor extends SimpleFileVisitor<Path> {
        private Map<String, ResourceCache> oldMap;
        private Map<String, ResourceCache> map;
        private Path root;

        LoadResourceVisitor(Map<String, ResourceCache> oldMap) {
            this.oldMap = oldMap;
            this.map = Maps.newHashMapWithExpectedSize(oldMap.size());
        }

        @Override
        public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
            if (root == null) {
                root = dir;
            }
            return super.preVisitDirectory(dir, attrs);
        }

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
            if (file.getFileName().toString().startsWith(".")) {
                return CONTINUE;
            }

            String path = file.toString();

            // Jar
            if (StringUtils.containsIgnoreCase(file.getFileSystem().getClass().getSimpleName(), "zip")) {
                String relativePath = path;
                if (path.startsWith(ResourceWalker.SPRING_BOOT_CLASSES)) {
                    relativePath = path.substring(ResourceWalker.SPRING_BOOT_CLASSES.length());
                }

                InputStream in = getClass().getResourceAsStream("classpath:" + relativePath);
                byte[] content;
                try {
                    content = StreamUtils.copyToByteArray(in);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                } finally {
                    IOUtil.closeQuietly(in);
                }
                String md5 = DigestUtils.md5DigestAsHex(content);
                String mimeType = MediaTypeFactory.getMediaType(relativePath).orElse(MediaType.APPLICATION_OCTET_STREAM).toString();
                ResourceCache resourceCache = new ResourceCache();
                resourceCache.setContent(content);
                resourceCache.setMd5(md5);
                resourceCache.setMimeType(mimeType);
                resourceCache.setLastModified(attrs.lastModifiedTime().toMillis());
                map.put(relativePath, resourceCache);
                return CONTINUE;
            }

            // File
            String relativePath = STATIC_ROOT + "/" + root.relativize(file).toString();
            ResourceCache old = oldMap.get(path);
            long lastModified = attrs.lastModifiedTime().toMillis();
            if (old != null && old.getLastModified() >= lastModified) {
                map.put(relativePath, old);
                return CONTINUE;
            }

            File f = new File(path);
            byte[] content;
            try {
                content = FileUtil.toByteArray(f);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            String md5 = DigestUtils.md5DigestAsHex(content);
            String mimeType = MediaTypeFactory.getMediaType(f.getName()).orElse(MediaType.APPLICATION_OCTET_STREAM).toString();
            ResourceCache resourceCache = new ResourceCache();
            resourceCache.setContent(content);
            resourceCache.setMd5(md5);
            resourceCache.setMimeType(mimeType);
            resourceCache.setLastModified(lastModified);
            map.put(relativePath, resourceCache);
            return CONTINUE;
        }

        public Map<String, ResourceCache> getMap() {
            return map;
        }
    }

    static class LastModifiedVisitor extends SimpleFileVisitor<Path> {

        private long lastModified;

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
            if (file.getFileName().toString().startsWith(".")) {
                return CONTINUE;
            }

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

    static class CssCache extends WebPageReferenceData {
        private String normalizeCssMd5;
        private String styleCssMd5;

        boolean checkNotModified(String normalizeCssMd5, String styleCssMd5) {
            return this.normalizeCssMd5.equals(normalizeCssMd5) && this.styleCssMd5.equals(styleCssMd5);
        }

        public String getNormalizeCssMd5() {
            return normalizeCssMd5;
        }

        public void setNormalizeCssMd5(String normalizeCssMd5) {
            this.normalizeCssMd5 = normalizeCssMd5;
        }

        public String getStyleCssMd5() {
            return styleCssMd5;
        }

        public void setStyleCssMd5(String styleCssMd5) {
            this.styleCssMd5 = styleCssMd5;
        }
    }


    static class JsCache extends WebPageReferenceData {

        private String treeJsonMd5;

        private String scriptJsMd5;


        boolean checkNotModified(String treeJsonMd5, String scriptJsMd5) {
            return this.treeJsonMd5.equals(treeJsonMd5) && this.scriptJsMd5.equals(scriptJsMd5);
        }

        public String getTreeJsonMd5() {
            return treeJsonMd5;
        }

        public void setTreeJsonMd5(String treeJsonMd5) {
            this.treeJsonMd5 = treeJsonMd5;
        }

        public String getScriptJsMd5() {
            return scriptJsMd5;
        }

        public void setScriptJsMd5(String scriptJsMd5) {
            this.scriptJsMd5 = scriptJsMd5;
        }
    }

    public static abstract class WebPageReferenceData {
        private String version;
        private byte[] content;
        private String etag;

        public String getVersion() {
            return version;
        }

        public byte[] getContent() {
            return content;
        }

        public String getEtag() {
            return etag;
        }

        public void setVersion(String version) {
            this.version = version;
        }

        public void setContent(byte[] content) {
            this.content = content;
        }

        public void setEtag(String etag) {
            this.etag = etag;
        }
    }

    static class ResourceCache {
        private byte[] content;
        private String md5; // content md5
        private String mimeType;
        private long lastModified; // file last modified

        public byte[] getContent() {
            return content;
        }

        public void setContent(byte[] content) {
            this.content = content;
        }

        public String getMd5() {
            return md5;
        }

        public void setMd5(String md5) {
            this.md5 = md5;
        }

        public String getMimeType() {
            return mimeType;
        }

        public void setMimeType(String mimeType) {
            this.mimeType = mimeType;
        }

        public long getLastModified() {
            return lastModified;
        }

        public void setLastModified(long lastModified) {
            this.lastModified = lastModified;
        }
    }

}
