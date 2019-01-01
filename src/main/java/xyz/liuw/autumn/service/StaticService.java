package xyz.liuw.autumn.service;

import com.vip.vjtools.vjkit.io.FileUtil;
import com.vip.vjtools.vjkit.text.StringBuilderHolder;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.DigestUtils;
import org.springframework.web.context.request.WebRequest;
import xyz.liuw.autumn.data.Page;
import xyz.liuw.autumn.data.PageParser;
import xyz.liuw.autumn.data.ResourceLoader;
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
import static xyz.liuw.autumn.data.ResourceLoader.WEBJARS_ROOT;

/**
 * @author liuwei
 * Created by liuwei on 2018/11/19.
 */
@Component
public class StaticService {
    private static Logger logger = LoggerFactory.getLogger(StaticService.class);
    private static StringBuilderHolder stringBuilderHolder = new StringBuilderHolder(1024);
    private volatile JsCache jsCache;
    private volatile CssCache cssCache;
    private volatile Page helpPage;
    private String codeHighlightJs;
    private String codeHighlightCss;
    private List<ResourceLoader.StaticChangedListener> staticChangedListeners = new ArrayList<>(1);
    @Value("${autumn.code-block-highlighting.enabled}")
    private boolean codeHighlightEnabled;

    @Value("${autumn.code-block-highlighting.languages}")
    private List<String> highlightLanguages;

    @Value("${autumn.code-block-highlighting.style}")
    private String highlightStyle;

    @Autowired
    private ResourceLoader resourceLoader;

    @Autowired
    private JsCssCompressor jsCssCompressor;

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
        String etag = WebUtil.getEtag(resourceCache.getMd5());
        if (WebUtil.checkNotModified(webRequest, etag)) {
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

    public WebPageReferenceData getJsCache() {
        return jsCache;
    }

    public WebPageReferenceData getCssCache() {
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

        if (jsCache == null || jsCache.hasChanged(scriptJs.getMd5(), quickSearchJs.getMd5())) {
            jsCache = createJsCache(scriptJs, quickSearchJs);
            logger.info("jsCache updated");
            changed = true;
        }
        return changed;
    }

    @SuppressWarnings("SameParameterValue")
    private JsCache createJsCache(ResourceLoader.ResourceCache scriptJs,
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

        StringBuilder stringBuilder = stringBuilderHolder.get();
        stringBuilder.append("\"use strict\";\n")
                .append("(function () {\n")
                .append(scriptJsContent).append("\n")
                .append(quickSearchJsContent).append("\n")
                .append("})();");
        if (codeHighlightEnabled) {
            stringBuilder.append("\n").append(getCodeHighlightJs());
        }

        String js = jsCssCompressor.compressJs(stringBuilder.toString());

        byte[] jsBytes = js.getBytes(UTF_8);
        String md5 = DigestUtils.md5DigestAsHex(jsBytes);
        String etag = WebUtil.getEtag(md5);

        JsCache jsCache = new JsCache();
        jsCache.setContent(jsBytes);
        jsCache.setEtag(etag);
        jsCache.setVersionKeyValue(WebUtil.getVersionKeyValue(md5));
        jsCache.setScriptJsMd5(scriptJs.getMd5());
        jsCache.setQuickSearchJsMd5(quickSearchJs.getMd5());
        return jsCache;
    }

    private boolean refreshCssCache() {
        ResourceLoader.ResourceCache normalizeCss = getResourceCache("/css/normalize.css");
        ResourceLoader.ResourceCache styleCss = getResourceCache("/css/style.css");
        if (cssCache != null && cssCache.checkNotModified(normalizeCss.getMd5(), styleCss.getMd5())) {
            return false;
        }

        StringBuilder stringBuilder = stringBuilderHolder.get();
        stringBuilder.append(new String(normalizeCss.getContent(), UTF_8))
                .append(new String(styleCss.getContent(), UTF_8));
        if (codeHighlightEnabled) {
            stringBuilder.append("\n").append(getCodeHighlightCss());
        }
        String cssText = stringBuilder.toString();

        CssCache cssCache = new CssCache();
        cssCache.setContent(cssText.getBytes(UTF_8));
        String md5 = DigestUtils.md5DigestAsHex(cssCache.getContent());
        String etag = WebUtil.getEtag(md5);
        cssCache.setEtag(etag);
        cssCache.setVersionKeyValue(WebUtil.getVersionKeyValue(md5));
        cssCache.setNormalizeCssMd5(normalizeCss.getMd5());
        cssCache.setStyleCssMd5(styleCss.getMd5());
        this.cssCache = cssCache;
        logger.info("cssCache updated");
        return true;
    }

    private String getCodeHighlightJs() {
        if (codeHighlightJs != null) {
            return codeHighlightJs;
        }

        ResourceLoader.ResourceCache hljsCache = resourceLoader.getResourceCache(WEBJARS_ROOT + "/highlightjs/9.8.0/highlight.js");

        StringBuilder stringBuilder = StringBuilderHolder.getGlobal();
        stringBuilder.append(new String(hljsCache.getContent(), UTF_8)).append("\n");
        highlightLanguages.forEach(language -> {
            String classpath = WEBJARS_ROOT + "/highlightjs/9.8.0/languages/" + language + ".js";
            ResourceLoader.ResourceCache cache = resourceLoader.getResourceCache(classpath);
            String text = new String(cache.getContent(), UTF_8);
            String js = text.substring(text.indexOf("function"));
            // hljs.registerLanguage('language', function(hljs){...});
            stringBuilder.append("hljs.registerLanguage('").append(language).append("', ").append(js).append(");\n");
        });
        stringBuilder
                .append("window.addEventListener('load', function () {\n")
                .append("    Array.prototype.map.call(document.getElementsByClassName('language-text'),e => e)\n" +
                        "        .forEach(el => el.classList.remove('language-text'));\n")
                .append("    hljs.initHighlighting();\n")
                .append("});\n");

        return (codeHighlightJs = stringBuilder.toString());
    }

    private String getCodeHighlightCss() {
        if (codeHighlightCss != null) {
            return codeHighlightCss;
        }

        if (StringUtils.isBlank(highlightStyle)) {
            return (codeHighlightCss = "");
        }

        String webjarClasspath = WEBJARS_ROOT + "/highlightjs/9.8.0/styles/" + highlightStyle + ".css";
        String classpath = highlightStyle.startsWith("/") ? highlightStyle : webjarClasspath;
        ResourceLoader.ResourceCache cssCache = resourceLoader.getResourceCache(classpath);
        return (codeHighlightCss = new String(cssCache.getContent(), UTF_8));
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

        boolean hasChanged(String scriptJsMd5, String quickSearchJsMd5) {
            return !StringUtils.equals(this.scriptJsMd5, scriptJsMd5)
                    || !StringUtils.equals(this.quickSearchJsMd5, quickSearchJsMd5);
        }

        void setQuickSearchJsMd5(String quickSearchJsMd5) {
            this.quickSearchJsMd5 = quickSearchJsMd5;
        }

        void setScriptJsMd5(String scriptJsMd5) {
            this.scriptJsMd5 = scriptJsMd5;
        }
    }

    public static class WebPageReferenceData {
        private String versionKeyValue;
        private byte[] content;
        private String etag;

        String getVersionKeyValue() {
            return versionKeyValue;
        }

        public byte[] getContent() {
            return content;
        }

        public String getEtag() {
            return etag;
        }

        void setVersionKeyValue(String versionKeyValue) {
            this.versionKeyValue = versionKeyValue;
        }

        public void setContent(byte[] content) {
            this.content = content;
        }

        void setEtag(String etag) {
            this.etag = etag;
        }
    }
}
