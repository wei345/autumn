package io.liuwei.autumn.service;

import com.vip.vjtools.vjkit.text.StringBuilderHolder;
import io.liuwei.autumn.component.MediaRevisionResolver;
import io.liuwei.autumn.converter.ContentHtmlConverter;
import io.liuwei.autumn.dao.ResourceFileDao;
import io.liuwei.autumn.model.ContentHtml;
import io.liuwei.autumn.model.RevisionContent;
import io.liuwei.autumn.util.JsCompressor;
import io.liuwei.autumn.util.RevisionContentUtil;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.liuwei.autumn.dao.ResourceFileDao.STATIC_ROOT;

/**
 * @author liuwei
 * Created by liuwei on 2018/11/19.
 */
@Component
public class StaticService {
    private static final Logger logger = LoggerFactory.getLogger(StaticService.class);
    private static final StringBuilderHolder STRING_BUILDER_HOLDER = new StringBuilderHolder(1024);
    private final List<ResourceFileDao.StaticChangedListener> staticChangedListeners = new ArrayList<>(1);
    @Getter
    private volatile RevisionContent jsCache;
    @Getter
    private volatile RevisionContent cssCache;
    @Getter
    private volatile ContentHtml helpCache;
    private String codeBlockLineNumberJs;
    private String codeBlockHighlightJs;
    private String codeBlockHighlightCss;
    @Value("${autumn.code-block-line-number.enabled}")
    private boolean codeBlockLineNumberEnabled;

    @Value("${autumn.code-block-highlighting.enabled}")
    private boolean codeBlockHighlightEnabled;

    @Value("${autumn.highlightjs-version}")
    private String highlightjsVersion;

    @Value("${autumn.google-analytics-id}")
    private String googleAnalyticsId;

    @Value("${autumn.code-block-highlighting.languages}")
    private List<String> highlightLanguages;

    @Value("${autumn.code-block-highlighting.style}")
    private String codeBlockHighlightStyle;

    @Value("${autumn.compressor.javascript.enabled}")
    private boolean jsCompressEnabled;

    @Autowired
    private ResourceFileDao resourceFileDao;

    @Autowired
    private MediaRevisionResolver mediaRevisionResolver;

    @Autowired
    private ContentHtmlConverter contentHtmlConverter;

    @PostConstruct
    private void init() {
        refreshJsCache();
        refreshCssCache();
        refreshHelpCache();

        resourceFileDao.addStaticChangedListener(() -> {
            refreshHelpCache();
            if (refreshJsCache() || refreshCssCache()) {
                staticChangedListeners.forEach(ResourceFileDao.StaticChangedListener::onChanged);
            }
        });
    }

    public ResourceFileDao.ResourceCache getStaticResourceCache(String path) {
        return resourceFileDao.getResourceCache(STATIC_ROOT + path);
    }


    private boolean refreshJsCache() {
        boolean changed = false;

        List<ResourceFileDao.ResourceCache> sourceList = Stream.of(
                "/js/script.js", "/js/quick_search.js", "/js/util.js")
                .map(this::getStaticResourceCache).collect(Collectors.toList());

        List<Long> sourceTimeList = sourceList.stream()
                .map(ResourceFileDao.ResourceCache::getLastModified).collect(Collectors.toList());

        if (jsCache == null || checkChanged(sourceTimeList, jsCache)) {
            jsCache = createJsCache(sourceList);
            logger.info("jsCache updated");
            changed = true;
        }
        return changed;
    }

    @SuppressWarnings("SameParameterValue")
    private RevisionContent createJsCache(List<ResourceFileDao.ResourceCache> sourceList) {

        // 如果把 tree.js 也加进来：
        // 每次浏览器打开页面少发一个请求
        // 如果 tree.js 更新，那么其余 js 也跟着重新加载。tree.js 更新相对频繁

        StringBuilder stringBuilder = STRING_BUILDER_HOLDER.get();
        stringBuilder.append("\"use strict\";\n")
                .append("(function () {\n");
        sourceList.forEach(resourceCache ->
                stringBuilder.append(
                        resourceCache.getContentString()
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

        String js = stringBuilder.toString();
        if (jsCompressEnabled) {
            js = JsCompressor.compressJs("var autumn = {ctx: '', prefix: '', treeVersionKeyValue: ''}", js);
        }

        if (StringUtils.isNotBlank(googleAnalyticsId)) {
            js += getGoogleAnalyticsJs();
        }

        return RevisionContentUtil.newRevisionContent(js, mediaRevisionResolver);
    }

    private boolean refreshCssCache() {
        List<ResourceFileDao.ResourceCache> sourceList = Stream.of("/css/lib/normalize.css", "/css/style.css")
                .map(this::getStaticResourceCache).collect(Collectors.toList());
        List<Long> sourceTimeList = sourceList.stream().map(ResourceFileDao.ResourceCache::getLastModified).collect(Collectors.toList());
        if (cssCache != null && !checkChanged(sourceTimeList, cssCache)) {
            return false;
        }

        StringBuilder stringBuilder = STRING_BUILDER_HOLDER.get();
        sourceList.forEach(resourceCache ->
                stringBuilder.append(resourceCache.getContentString()).append("\n"));
        if (codeBlockHighlightEnabled) {
            stringBuilder.append(getCodeBlockHighlightCss()).append("\n");
        }

        this.cssCache = RevisionContentUtil.newRevisionContent(stringBuilder.toString(), mediaRevisionResolver);
        logger.info("cssCache updated");
        return true;
    }

    private boolean refreshHelpCache() {
        ResourceFileDao.ResourceCache resourceCache = getStaticResourceCache("/help.adoc");
        if (resourceCache == null) {
            return false;
        }

        if (helpCache != null && helpCache.getTime() >= resourceCache.getLastModified()) {
            return false;
        }

        this.helpCache = contentHtmlConverter.convert("Help", resourceCache.getContentString());
        logger.info("helpCache updated");
        return true;
    }

    private String getCodeBlockLineNumberJs() {
        if (codeBlockLineNumberJs != null) {
            return codeBlockLineNumberJs;
        }

        StringBuilder stringBuilder = new StringBuilder(7000);
        ResourceFileDao.ResourceCache resourceCache = getStaticResourceCache("/js/lib/highlightjs-line-numbers.js");
        // 不依赖 highlight.js
        stringBuilder.append("if(!window.hljs) window.hljs = {};\n")
                .append(resourceCache.getContentString()).append("\n")
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

        String hljsContent = resourceFileDao.getWebJarResourceAsString("/highlightjs/" + highlightjsVersion + "/highlight.js");

        StringBuilder stringBuilder = StringBuilderHolder.getGlobal();
        stringBuilder.append(hljsContent).append("\n");
        highlightLanguages.forEach(language -> {
            String text = resourceFileDao.getWebJarResourceAsString("/highlightjs/" + highlightjsVersion + "/languages/" + language + ".js");
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

    private String getGoogleAnalyticsJs() {
        return "window['GoogleAnalyticsObject'] = 'ga'; window.ga = {" +
                "q: [['create', '" + googleAnalyticsId + "', 'auto'], ['send', 'pageview']], " +
                "l: 1 * new Date()};\n" +
                getStaticResourceCache("/js/lib/google-analytics.js").getContentString();
    }

    private String getCodeBlockHighlightCss() {
        if (codeBlockHighlightCss != null) {
            return codeBlockHighlightCss;
        }

        if (StringUtils.isBlank(codeBlockHighlightStyle)) {
            return (codeBlockHighlightCss = "");
        }

        String text = resourceFileDao.getWebJarResourceAsString("/highlightjs/" + highlightjsVersion + "/styles/" + codeBlockHighlightStyle + ".css");
        return codeBlockHighlightCss = text;
    }

    void addStaticChangedListener(ResourceFileDao.StaticChangedListener listener) {
        this.staticChangedListeners.add(listener);
    }

    boolean checkChanged(List<Long> sourceTimeList, RevisionContent revisionContent) {
        Optional<Long> optionalLong = sourceTimeList.stream().max(Long::compareTo);
        return optionalLong.isPresent() && optionalLong.get() > revisionContent.getTimestamp();
    }

}
