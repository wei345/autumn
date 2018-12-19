package xyz.liuw.autumn.service;

import com.vip.vjtools.vjkit.io.FileUtil;
import com.vip.vjtools.vjkit.text.StringBuilderHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.DigestUtils;
import org.springframework.web.context.request.WebRequest;
import xyz.liuw.autumn.data.DataLoader;
import xyz.liuw.autumn.data.DataSource;
import xyz.liuw.autumn.data.ResourceLoader;
import xyz.liuw.autumn.util.WebUtil;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import static java.nio.charset.StandardCharsets.UTF_8;
import static xyz.liuw.autumn.data.ResourceLoader.STATIC_ROOT;
import static xyz.liuw.autumn.service.UserService.isLogged;

/**
 * @author liuwei
 * Created by liuwei on 2018/11/19.
 */
@Component
public class ResourceService {
    private static Logger logger = LoggerFactory.getLogger(ResourceService.class);

    private volatile JsCache userJsCache;

    private volatile JsCache guestJsCache;

    private volatile CssCache cssCache;

    @Autowired
    private WebUtil webUtil;

    @Autowired
    private DataLoader dataLoader;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private ResourceLoader resourceLoader;

    private volatile long templateLastModified;

    @PostConstruct
    private void init() {
        templateLastModified = resourceLoader.getTemplateLastModified();
        refreshJsCache();
        refreshCssCache();

        dataLoader.addListener(this::refreshJsCache);
        resourceLoader.addStaticChangedListener(() -> {
            refreshJsCache();
            refreshCssCache();
        });
        resourceLoader.addTemplateLastChangedListener(() -> {
            synchronized (this) {
                long v = resourceLoader.getTemplateLastModified();
                if (v > templateLastModified) {
                    templateLastModified = v;
                }
            }
        });
    }

    public Object handleStaticRequest(String path,
                                      WebRequest webRequest,
                                      HttpServletRequest request,
                                      HttpServletResponse response) throws IOException {

        ResourceLoader.ResourceCache cache = resourceLoader.getResourceCache(STATIC_ROOT + path);

        if (cache == null) {
            response.sendError(HttpStatus.NOT_FOUND.value(), HttpStatus.NOT_FOUND.getReasonPhrase());
            return null;
        }

        return MediaService.output(
                cache.getContent(),
                webUtil.getEtag(cache.getMd5()),
                FileUtil.getFileName(path),
                cache.getMimeType(),
                webRequest,
                request,
                response);
    }

    long getTemplateLastModified() {
        return templateLastModified;
    }

    public JsCache getJsCache() {
        return isLogged() ? userJsCache : guestJsCache;
    }

    public CssCache getCssCache() {
        return cssCache;
    }

    private synchronized void refreshJsCache() {
        ResourceLoader.ResourceCache scriptJs = resourceLoader.getResourceCache(STATIC_ROOT + "/js/script.js");
        ResourceLoader.ResourceCache quickSearchJs = resourceLoader.getResourceCache(STATIC_ROOT + "/js/quick_search.js");

        String userTreeJsonMd5 = dataSource.getAllData().getTreeJson().getMd5();
        if (userJsCache == null || !userJsCache.checkNotModified(userTreeJsonMd5, scriptJs.getMd5(), quickSearchJs.getMd5())) {
            userJsCache = createJsCache(userTreeJsonMd5, scriptJs, quickSearchJs);
            templateLastModified = System.currentTimeMillis();
            logger.info("userJsCache updated");
        }

        String guestTreeJsonMd5 = dataSource.getPublishedData().getTreeJson().getMd5();
        if (guestJsCache == null || !guestJsCache.checkNotModified(guestTreeJsonMd5, scriptJs.getMd5(), quickSearchJs.getMd5())) {
            guestJsCache = createJsCache(guestTreeJsonMd5, scriptJs, quickSearchJs);
            templateLastModified = System.currentTimeMillis();
            logger.info("guestJsCache updated");
        }
    }

    private JsCache createJsCache(String treeJsonMd5, ResourceLoader.ResourceCache scriptJs, ResourceLoader.ResourceCache quickSearchJs) {

        String scriptJsContent = new String(scriptJs.getContent(), UTF_8)
                .replaceFirst("\"use strict\";\n", "")
                .replaceFirst("ctx: ''", "ctx: '" + webUtil.getContextPath() + "'")
                .replaceFirst("treeVersion: ''", "treeVersion: '" + treeJsonMd5.substring(0, 7) + "'")
                .trim();

        String quickSearchJsContent = new String(quickSearchJs.getContent(), UTF_8)
                .replaceFirst("\"use strict\";\n", "")
                .trim();

        byte[] jsBytes = StringBuilderHolder.getGlobal()
                .append("\"use strict\";\n")
                .append("(function () {\n")
                .append(scriptJsContent).append("\n")
                .append(quickSearchJsContent).append("\n")
                .append("})();")
                .toString()
                .getBytes(UTF_8);

        String md5 = DigestUtils.md5DigestAsHex(jsBytes);
        String etag = webUtil.getEtag(md5);
        String version = md5.substring(0, 7);

        JsCache jsCache = new JsCache();
        jsCache.setContent(jsBytes);
        jsCache.setEtag(etag);
        jsCache.setVersion(version);
        jsCache.setTreeJsonMd5(treeJsonMd5);
        jsCache.setScriptJsMd5(scriptJs.getMd5());
        jsCache.setQuickSearchJsMd5(quickSearchJs.getMd5());
        return jsCache;
    }

    private synchronized void refreshCssCache() {
        ResourceLoader.ResourceCache normalizeCss = resourceLoader.getResourceCache(STATIC_ROOT + "/css/normalize.css");
        ResourceLoader.ResourceCache styleCss = resourceLoader.getResourceCache(STATIC_ROOT + "/css/style.css");
        if (cssCache != null && cssCache.checkNotModified(normalizeCss.getMd5(), styleCss.getMd5())) {
            return;
        }

        String cssText = new String(normalizeCss.getContent(), UTF_8) + new String(styleCss.getContent(), UTF_8);
        CssCache cssCache = new CssCache();
        cssCache.setContent(cssText.getBytes(UTF_8));
        String md5 = DigestUtils.md5DigestAsHex(cssCache.getContent());
        String etag = webUtil.getEtag(md5);
        String version = md5.substring(0, 7);
        cssCache.setEtag(etag);
        cssCache.setVersion(version);
        cssCache.setNormalizeCssMd5(normalizeCss.getMd5());
        cssCache.setStyleCssMd5(styleCss.getMd5());
        this.cssCache = cssCache;
        this.templateLastModified = System.currentTimeMillis();
        logger.info("cssCache updated");
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

        private String quickSearchJsMd5;

        private String scriptJsMd5;

        private String treeJsonMd5;


        boolean checkNotModified(String treeJsonMd5, String scriptJsMd5, String quickSearchJsMd5) {
            return this.treeJsonMd5.equals(treeJsonMd5)
                    && this.scriptJsMd5.equals(scriptJsMd5)
                    && this.quickSearchJsMd5.equals(quickSearchJsMd5);
        }

        public String getQuickSearchJsMd5() {
            return quickSearchJsMd5;
        }

        public void setQuickSearchJsMd5(String quickSearchJsMd5) {
            this.quickSearchJsMd5 = quickSearchJsMd5;
        }

        public String getScriptJsMd5() {
            return scriptJsMd5;
        }

        public void setScriptJsMd5(String scriptJsMd5) {
            this.scriptJsMd5 = scriptJsMd5;
        }

        public void setTreeJsonMd5(String treeJsonMd5) {
            this.treeJsonMd5 = treeJsonMd5;
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
}
