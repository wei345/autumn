package io.liuwei.autumn.service;

import com.vip.vjtools.vjkit.text.StringBuilderHolder;
import io.liuwei.autumn.component.MediaRevisionResolver;
import io.liuwei.autumn.config.AppProperties;
import io.liuwei.autumn.converter.ContentHtmlConverter;
import io.liuwei.autumn.manager.ResourceFileManager;
import io.liuwei.autumn.model.ContentHtml;
import io.liuwei.autumn.model.ResourceFile;
import io.liuwei.autumn.model.RevisionContent;
import io.liuwei.autumn.util.IOUtil;
import io.liuwei.autumn.util.JsCompressor;
import io.liuwei.autumn.util.RevisionContentUtil;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;


/**
 * @author liuwei
 * Created by liuwei on 2018/11/19.
 */
@SuppressWarnings("UnusedReturnValue")
@Component
@Slf4j
public class StaticService {
    private static final StringBuilderHolder STRING_BUILDER_HOLDER = new StringBuilderHolder(1024);

    @Autowired
    private ResourceFileManager resourceFileManager;

    @Autowired
    private MediaRevisionResolver mediaRevisionResolver;

    @Autowired
    private ContentHtmlConverter contentHtmlConverter;

    private AppProperties.CodeBlock codeBlock;

    @Value("${autumn.google-analytics-id}")
    private String googleAnalyticsId;

    @Value("${autumn.compressor.javascript.enabled}")
    private boolean jsCompressEnabled;

    @Getter
    private volatile RevisionContent jsCache;

    @Getter
    private volatile RevisionContent cssCache;

    @Getter
    private volatile ContentHtml helpCache;

    @Autowired
    private void setAppProperties(AppProperties appProperties) {
        this.codeBlock = appProperties.getCodeBlock();
    }

    @PostConstruct
    private void init() {
        refresh();
        resourceFileManager.addStaticChangedListener(this::refresh);
    }

    private void refresh() {
        refreshJsCache();
        refreshCssCache();
        refreshHelpCache();
    }

    private boolean refreshJsCache() {
        List<ResourceFile> jsList = Stream
                .of("/js/script.js", "/js/quick_search.js", "/js/util.js")
                .map(this::getStaticResourceFile)
                .collect(Collectors.toList());

        List<Long> jsLastModifiedList = jsList
                .stream()
                .map(ResourceFile::getLastModified)
                .collect(Collectors.toList());

        if (jsCache == null || checkChanged(jsLastModifiedList, jsCache)) {
            jsCache = createJsCache(jsList);
            log.info("jsCache updated");
            return true;
        }
        return false;
    }

    private RevisionContent createJsCache(List<ResourceFile> jsList) {

        StringBuilder sb = STRING_BUILDER_HOLDER.get();

        // 我们的 js，包到一个 function 里
        sb
                .append("\"use strict\";\n")
                .append("(function () {\n");
        jsList.forEach(js ->
                sb
                        .append(js.getContentString().replaceFirst("\"use strict\";\n", "").trim())
                        .append("\n"));
        sb.append("})();\n");

        // 代码块高亮
        if (codeBlock.isHighlightingEnabled()) {
            sb
                    .append(getHighlightJs())
                    .append("\n");
        }

        // 代码块行号
        if (codeBlock.isLineNumberEnabled()) {
            sb
                    .append(getLineNumberJs())
                    .append("\n");
        }

        // Google 分析
        if (StringUtils.isNotBlank(googleAnalyticsId)) {
            sb
                    .append(getGoogleAnalyticsJs())
                    .append("\n");
        }

        // 压缩
        String content = sb.toString();
        if (jsCompressEnabled) {
            String depend = "var autumn = {ctx: '', prefix: '', treeVersionKeyValue: ''}";
            content = JsCompressor.compressJs(depend, content);
        }

        return RevisionContentUtil.newRevisionContent(content, mediaRevisionResolver);
    }

    private boolean refreshCssCache() {
        List<ResourceFile> cssList = Stream
                .of("/css/lib/normalize.css", "/css/style.css")
                .map(this::getStaticResourceFile)
                .collect(Collectors.toList());

        List<Long> cssLastModifiedList = cssList
                .stream()
                .map(ResourceFile::getLastModified)
                .collect(Collectors.toList());

        if (cssCache == null || checkChanged(cssLastModifiedList, cssCache)) {
            StringBuilder stringBuilder = STRING_BUILDER_HOLDER.get();

            // 我们的 css
            cssList.forEach(css ->
                    stringBuilder
                            .append(css.getContentString())
                            .append("\n"));

            // 代码块高亮
            if (codeBlock.isHighlightingEnabled()) {
                stringBuilder
                        .append(IOUtil.resourceToString(getHighlightJsCssPath()))
                        .append("\n");
            }

            this.cssCache = RevisionContentUtil.newRevisionContent(stringBuilder.toString(), mediaRevisionResolver);
            log.info("cssCache updated");
            return true;
        }

        return false;
    }

    private boolean refreshHelpCache() {
        ResourceFile help = getStaticResourceFile("/help.adoc");
        if (help == null) {
            return false;
        }

        if (helpCache == null || helpCache.getTime() < help.getLastModified()) {
            this.helpCache = contentHtmlConverter.convert("Help", help.getContentString());
            log.info("helpCache updated");
            return true;
        }
        return false;
    }

    private String getLineNumberJs() {
        StringBuilder stringBuilder = new StringBuilder(7000);
        ResourceFile js = getStaticResourceFile("/js/lib/highlightjs-line-numbers.js");
        // 不依赖 highlight.js
        return stringBuilder.append("if(!window.hljs) window.hljs = {};\n")
                .append(js.getContentString()).append("\n")
                .append("window.addEventListener('load', function () {\n")
                .append("    Array.prototype.map.call(document.querySelectorAll('pre code'), el => el)\n")
                .append("        .forEach(block => hljs.lineNumbersBlock(block));\n")
                .append("});\n")
                .toString();
    }

    private String getHighlightJs() {
        StringBuilder stringBuilder = StringBuilderHolder.getGlobal();

        stringBuilder
                .append(IOUtil.resourceToString(getHighlightJsPath()))
                .append("\n");

        codeBlock
                .getHighlightingLanguages()
                .forEach(language -> {
                    String content = IOUtil.resourceToString(getHighlightJsLanguagePath(language));
                    String function = content.substring(content.indexOf("function"));

                    // hljs.registerLanguage('language', function(hljs){...});
                    stringBuilder
                            .append("hljs.registerLanguage('")
                            .append(language)
                            .append("', ")
                            .append(function)
                            .append(");\n");
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

        return stringBuilder.toString();
    }

    private String getHighlightJsPath() {
        return getHighlightJsBasePath() + "/highlight.js";
    }

    private String getHighlightJsLanguagePath(String language) {
        return getHighlightJsBasePath() + "/languages/" + language + ".js";
    }

    private String getHighlightJsCssPath() {
        return getHighlightJsBasePath() + "/styles/" + codeBlock.getHighlightingStyle() + ".css";
    }

    private String getHighlightJsBasePath() {
        return "/META-INF/resources/webjars/highlightjs/" + codeBlock.getHighlightjsVersion();
    }

    private String getGoogleAnalyticsJs() {
        return "window['GoogleAnalyticsObject'] = 'ga'; window.ga = {" +
                "q: [['create', '" + googleAnalyticsId + "', 'auto'], ['send', 'pageview']], " +
                "l: 1 * new Date()};\n" +
                getStaticResourceFile("/js/lib/google-analytics.js").getContentString();
    }

    boolean checkChanged(List<Long> lastModifiedList, RevisionContent revisionContent) {
        Optional<Long> optionalLong = lastModifiedList.stream().max(Long::compareTo);
        return optionalLong.isPresent() && optionalLong.get() > revisionContent.getTimestamp();
    }

    private ResourceFile getStaticResourceFile(String path) {
        return resourceFileManager.getResourceFile(resourceFileManager.getStaticRoot() + path);
    }

}
