package xyz.liuw.autumn.service;

import com.vip.vjtools.vjkit.io.FileUtil;
import com.vip.vjtools.vjkit.text.StringBuilderHolder;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.util.DigestUtils;
import org.springframework.web.context.request.WebRequest;
import xyz.liuw.autumn.data.*;
import xyz.liuw.autumn.util.WebUtil;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.constraints.NotNull;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static java.nio.charset.StandardCharsets.UTF_8;
import static xyz.liuw.autumn.data.ResourceLoader.STATIC_ROOT;
import static xyz.liuw.autumn.service.UserService.isLogged;

/**
 * @author liuwei
 * Created by liuwei on 2018/11/19.
 */
@Component
public class StaticService {
    private static Logger logger = LoggerFactory.getLogger(StaticService.class);

    // 如果 js 中包含 tree.js 或 treeVersion，那么 userJsCache 与 guestJsCache 不一样，否则二者一样。
    private volatile JsCache userJsCache;

    private volatile JsCache guestJsCache;

    private volatile CssCache cssCache;

    private volatile Page helpPage;

    @Autowired
    private WebUtil webUtil;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private ResourceLoader resourceLoader;

    private List<ResourceLoader.StaticChangedListener> staticChangedListeners = new ArrayList<>(1);

    @PostConstruct
    private void init() {
        refreshJsCache();
        refreshCssCache();
        refreshHelpPage();

        resourceLoader.addStaticChangedListener(() -> {
            refreshHelpPage();
            if (refreshJsCache() || refreshCssCache()) {
                staticChangedListeners.forEach(ResourceLoader.StaticChangedListener::onChanged);
            }
        });
    }

    public Object handleRequest(@NotNull ResourceLoader.ResourceCache resourceCache,
                                WebRequest webRequest,
                                HttpServletRequest request,
                                HttpServletResponse response) {
        String etag = webUtil.getEtag(resourceCache.getMd5());
        if (webRequest.checkNotModified(etag)) {
            return null;
        }

        return MediaService.handleRequest(
                resourceCache.getContent(),
                etag,
                FileUtil.getFileName(resourceCache.getPath()),
                resourceCache.getMimeType(),
                webRequest,
                request
        );
    }

    public ResourceLoader.ResourceCache getResourceCache(String path) {
        return resourceLoader.getResourceCache(STATIC_ROOT + path);
    }

    public JsCache getJsCache() {
        return isLogged() ? userJsCache : guestJsCache;
    }

    public CssCache getCssCache() {
        return cssCache;
    }

    Page getHelpPage() {
        return helpPage;
    }

    private void refreshHelpPage() {
        ResourceLoader.ResourceCache data = getResourceCache("/help.md");
        if (helpPage != null && helpPage.getLastModified() >= data.getLastModified()) {
            return;
        }

        Page page = newPageOf(data);
        page.setPath("/help");
        helpPage = page;
        logger.info("Updated {}", page.getPath());
    }

    private Page newPageOf(ResourceLoader.ResourceCache resourceCache) {
        String content = new String(resourceCache.getContent(), StandardCharsets.UTF_8);
        Page page = PageParser.parse(content);
        Date date = new Date(resourceCache.getLastModified());
        page.setCreated(date);
        page.setModified(date);
        page.setLastModified(date.getTime());
        return page;
    }

    private boolean refreshJsCache() {
        boolean changed = false;
        ResourceLoader.ResourceCache scriptJs = getResourceCache("/js/script.js");
        ResourceLoader.ResourceCache quickSearchJs = getResourceCache("/js/quick_search.js");

        if (userJsCache == null || userJsCache.hasChanged(null, scriptJs.getMd5(), quickSearchJs.getMd5())) {
            userJsCache = createJsCache(null, scriptJs, quickSearchJs);
            logger.info("userJsCache updated");
            changed = true;
        }

        if (guestJsCache == null || guestJsCache.hasChanged(null, scriptJs.getMd5(), quickSearchJs.getMd5())) {
            guestJsCache = createJsCache(null, scriptJs, quickSearchJs);
            logger.info("guestJsCache updated");
            changed = true;
        }
        return changed;
    }

    @SuppressWarnings("SameParameterValue")
    private JsCache createJsCache(@Nullable TreeJson treeJson,
                                  ResourceLoader.ResourceCache scriptJs,
                                  ResourceLoader.ResourceCache quickSearchJs) {

        // 如果把 tree.js 也加进来：
        // 每次浏览器打开页面少发一个请求
        // 如果 tree.js 更新，那么其余 js 也跟着重新加载。tree.js 更新相对频繁，大约一周 2 次

        String scriptJsContent = new String(scriptJs.getContent(), UTF_8)
                .replaceFirst("\"use strict\";\n", "")
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
        jsCache.setScriptJsMd5(scriptJs.getMd5());
        jsCache.setQuickSearchJsMd5(quickSearchJs.getMd5());
        if (treeJson != null) {
            jsCache.setTreeJsonMd5(treeJson.getMd5());
        }
        return jsCache;
    }

    private boolean refreshCssCache() {
        ResourceLoader.ResourceCache normalizeCss = getResourceCache("/css/normalize.css");
        ResourceLoader.ResourceCache styleCss = getResourceCache("/css/style.css");
        if (cssCache != null && cssCache.checkNotModified(normalizeCss.getMd5(), styleCss.getMd5())) {
            return false;
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
        logger.info("cssCache updated");
        return true;
    }

    void addStaticChangedListener(ResourceLoader.StaticChangedListener listener) {
        this.staticChangedListeners.add(listener);
    }

    static class CssCache extends WebPageReferenceData {
        private String normalizeCssMd5;
        private String styleCssMd5;

        boolean checkNotModified(String normalizeCssMd5, String styleCssMd5) {
            return this.normalizeCssMd5.equals(normalizeCssMd5) && this.styleCssMd5.equals(styleCssMd5);
        }

        void setNormalizeCssMd5(String normalizeCssMd5) {
            this.normalizeCssMd5 = normalizeCssMd5;
        }

        void setStyleCssMd5(String styleCssMd5) {
            this.styleCssMd5 = styleCssMd5;
        }
    }

    static class JsCache extends WebPageReferenceData {

        private String quickSearchJsMd5;

        private String scriptJsMd5;

        private String treeJsonMd5;

        boolean hasChanged(String treeJsonMd5, String scriptJsMd5, String quickSearchJsMd5) {
            return !StringUtils.equals(this.treeJsonMd5, treeJsonMd5)
                    || !StringUtils.equals(this.scriptJsMd5, scriptJsMd5)
                    || !StringUtils.equals(this.quickSearchJsMd5, quickSearchJsMd5);
        }

        void setQuickSearchJsMd5(String quickSearchJsMd5) {
            this.quickSearchJsMd5 = quickSearchJsMd5;
        }

        void setScriptJsMd5(String scriptJsMd5) {
            this.scriptJsMd5 = scriptJsMd5;
        }

        void setTreeJsonMd5(String treeJsonMd5) {
            this.treeJsonMd5 = treeJsonMd5;
        }
    }

    public static abstract class WebPageReferenceData {
        private String version;
        private byte[] content;
        private String etag;

        String getVersion() {
            return version;
        }

        public byte[] getContent() {
            return content;
        }

        public String getEtag() {
            return etag;
        }

        void setVersion(String version) {
            this.version = version;
        }

        public void setContent(byte[] content) {
            this.content = content;
        }

        void setEtag(String etag) {
            this.etag = etag;
        }
    }
}