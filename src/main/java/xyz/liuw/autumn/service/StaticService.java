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
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.nio.charset.StandardCharsets.UTF_8;
import static xyz.liuw.autumn.data.ResourceLoader.STATIC_ROOT;

/**
 * @author liuwei
 * Created by liuwei on 2018/11/19.
 */
@Component
public class StaticService {
    private static Logger logger = LoggerFactory.getLogger(StaticService.class);
    private static StringBuilderHolder stringBuilderHolder = new StringBuilderHolder(1024);
    private volatile WebPageReferenceData jsCache;
    private volatile WebPageReferenceData cssCache;
    private volatile Page helpPage;
    private String codeBlockLineNumberJs;
    private String codeBlockHighlightJs;
    private String codeBlockHighlightCss;
    private List<ResourceLoader.StaticChangedListener> staticChangedListeners = new ArrayList<>(1);

    @Value("${autumn.code-block-line-number.enabled}")
    private boolean codeBlockLineNumberEnabled;

    @Value("${autumn.code-block-highlighting.enabled}")
    private boolean codeBlockHighlightEnabled;

    @Value("${autumn.highlightjs-version}")
    private String highlightjsVersion;

    @Value("${autumn.code-block-highlighting.languages}")
    private List<String> highlightLanguages;

    @Value("${autumn.code-block-highlighting.style}")
    private String codeBlockHighlightStyle;

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

    public ResourceLoader.ResourceCache getStaticResourceCache(String path) {
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
        ResourceLoader.ResourceCache data = getStaticResourceCache("/help.md");
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

        List<ResourceLoader.ResourceCache> sourceList = Stream.of("/js/script.js", "/js/quick_search.js", "/js/util.js")
                .map(this::getStaticResourceCache).collect(Collectors.toList());

        List<Long> sourceTimeList = sourceList.stream().map(ResourceLoader.ResourceCache::getLastModified).collect(Collectors.toList());
        if (jsCache == null || jsCache.checkChanged(sourceTimeList)) {
            jsCache = createJsCache(sourceList);
            logger.info("jsCache updated");
            changed = true;
        }
        return changed;
    }

    @SuppressWarnings("SameParameterValue")
    private WebPageReferenceData createJsCache(List<ResourceLoader.ResourceCache> sourceList) {

        // 如果把 tree.js 也加进来：
        // 每次浏览器打开页面少发一个请求
        // 如果 tree.js 更新，那么其余 js 也跟着重新加载。tree.js 更新相对频繁

        StringBuilder stringBuilder = stringBuilderHolder.get();
        stringBuilder.append("\"use strict\";\n")
                .append("(function () {\n");
        sourceList.forEach(resourceCache ->
                stringBuilder.append(
                        new String(resourceCache.getContent(), UTF_8)
                                .replaceFirst("\"use strict\";\n", "")
                                .trim())
                        .append("\n"));
        stringBuilder.append("})();\n");
        if (codeBlockHighlightEnabled) {
            stringBuilder.append(getCodeBlockHighlightJs()).append("\n");
        }
        if (codeBlockLineNumberEnabled) {
            stringBuilder.append(getCodeBlockLineNumberJs()).append("\n");
        }

        String js = jsCssCompressor.compressJs(stringBuilder.toString());

        byte[] jsBytes = js.getBytes(UTF_8);
        String md5 = DigestUtils.md5DigestAsHex(jsBytes);
        String etag = WebUtil.getEtag(md5);

        WebPageReferenceData jsCache = new WebPageReferenceData();
        jsCache.setContent(jsBytes);
        jsCache.setEtag(etag);
        jsCache.setVersionKeyValue(WebUtil.getVersionKeyValue(md5));
        return jsCache;
    }

    private boolean refreshCssCache() {
        List<ResourceLoader.ResourceCache> sourceList = Stream.of("/css/lib/normalize.css", "/css/style.css")
                .map(this::getStaticResourceCache).collect(Collectors.toList());
        List<Long> sourceTimeList = sourceList.stream().map(ResourceLoader.ResourceCache::getLastModified).collect(Collectors.toList());
        if (cssCache != null && !cssCache.checkChanged(sourceTimeList)) {
            return false;
        }

        StringBuilder stringBuilder = stringBuilderHolder.get();
        sourceList.forEach(resourceCache ->
                stringBuilder.append(new String(resourceCache.getContent(), UTF_8)).append("\n"));
        if (codeBlockHighlightEnabled) {
            stringBuilder.append(getCodeBlockHighlightCss()).append("\n");
        }

        WebPageReferenceData cssCache = new WebPageReferenceData();
        cssCache.setContent(stringBuilder.toString().getBytes(UTF_8));
        String md5 = DigestUtils.md5DigestAsHex(cssCache.getContent());
        String etag = WebUtil.getEtag(md5);
        cssCache.setEtag(etag);
        cssCache.setVersionKeyValue(WebUtil.getVersionKeyValue(md5));
        this.cssCache = cssCache;
        logger.info("cssCache updated");
        return true;
    }

    private String getCodeBlockLineNumberJs() {
        if (codeBlockLineNumberJs != null) {
            return codeBlockLineNumberJs;
        }

        StringBuilder stringBuilder = new StringBuilder(7000);
        ResourceLoader.ResourceCache resourceCache = getStaticResourceCache("/js/lib/highlightjs-line-numbers.js");
        // 不依赖 highlight.js
        stringBuilder.append("if(!window.hljs) window.hljs = {};\n")
                .append(new String(resourceCache.getContent(), UTF_8)).append("\n")
                .append("window.addEventListener('load', function () {\n")
                .append("    Array.prototype.map.call(document.querySelectorAll('pre code'), el => el)\n")
                .append("        .forEach(block => hljs.lineNumbersBlock(block));\n")
                .append("});\n");
        return codeBlockLineNumberJs = stringBuilder.toString();
    }

    private String getCodeBlockHighlightJs() {
        if (codeBlockHighlightJs != null) {
            return codeBlockHighlightJs;
        }

        String hljsContent = resourceLoader.getWebJarResourceAsString("/highlightjs/" + highlightjsVersion + "/highlight.js");

        StringBuilder stringBuilder = StringBuilderHolder.getGlobal();
        stringBuilder.append(hljsContent).append("\n");
        highlightLanguages.forEach(language -> {
            String text = resourceLoader.getWebJarResourceAsString("/highlightjs/" + highlightjsVersion + "/languages/" + language + ".js");
            String js = text.substring(text.indexOf("function"));
            // hljs.registerLanguage('language', function(hljs){...});
            stringBuilder.append("hljs.registerLanguage('").append(language).append("', ").append(js).append(");\n");
        });
        stringBuilder
                .append("window.addEventListener('load', function () {\n")
                .append("    Array.prototype.map.call(document.querySelectorAll('pre code'), el => el)\n")
                .append("        .forEach(block => {\n")
                .append("            var isText = block.classList.contains('text');\n")
                .append("            if(isText) block.classList.remove('text');\n")
                .append("            hljs.highlightBlock(block);\n")
                .append("            if(isText) block.classList.add('text');\n")
                .append("        });\n")
                .append("});\n");

        return (codeBlockHighlightJs = stringBuilder.toString());
    }

    private String getCodeBlockHighlightCss() {
        if (codeBlockHighlightCss != null) {
            return codeBlockHighlightCss;
        }

        if (StringUtils.isBlank(codeBlockHighlightStyle)) {
            return (codeBlockHighlightCss = "");
        }

        String text = resourceLoader.getWebJarResourceAsString("/highlightjs/" + highlightjsVersion + "/styles/" + codeBlockHighlightStyle + ".css");
        return codeBlockHighlightCss = text;
    }

    void addStaticChangedListener(ResourceLoader.StaticChangedListener listener) {
        this.staticChangedListeners.add(listener);
    }

    public static class WebPageReferenceData {
        private String versionKeyValue;
        private byte[] content;
        private String etag;
        private long time;

        WebPageReferenceData() {
            time = System.currentTimeMillis();
        }

        boolean checkChanged(List<Long> sourceTimeList) {
            Optional<Long> optionalLong = sourceTimeList.stream().max(Long::compareTo);
            return optionalLong.isPresent() && optionalLong.get() > time;
        }

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
