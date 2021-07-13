package io.liuwei.autumn.service;

import com.vip.vjtools.vjkit.text.StringBuilderHolder;
import io.liuwei.autumn.component.MediaRevisionResolver;
import io.liuwei.autumn.config.AppProperties;
import io.liuwei.autumn.constant.CacheConstants;
import io.liuwei.autumn.constant.Constants;
import io.liuwei.autumn.converter.ContentHtmlConverter;
import io.liuwei.autumn.manager.ResourceFileManager;
import io.liuwei.autumn.model.ContentHtml;
import io.liuwei.autumn.model.ResourceFile;
import io.liuwei.autumn.model.RevisionContent;
import io.liuwei.autumn.util.IOUtil;
import io.liuwei.autumn.util.JsCompressor;
import io.liuwei.autumn.util.MediaTypeUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.List;
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

    @Autowired
    private void setAppProperties(AppProperties appProperties) {
        this.codeBlock = appProperties.getCodeBlock();
    }

    @Cacheable(value = CacheConstants.STATIC, key = "'" + Constants.JS_ALL_DOT_JS + "'")
    public RevisionContent getAllJs() {
        log.info("building {}", Constants.JS_ALL_DOT_JS);

        List<ResourceFile> jsList = Stream
                .of("/js/script.js", "/js/quick_search.js", "/js/util.js")
                .map(this::getStaticResourceFile)
                .collect(Collectors.toList());

        StringBuilder sb = STRING_BUILDER_HOLDER.get();

        // 我们的 js，包到一个 function 里
        sb
                .append("\"use strict\";\n")
                .append("(function () {\n");
        jsList.forEach(js ->
                sb
                        .append(js.getContentAsString().replaceFirst("\"use strict\";\n", "").trim())
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

        return mediaRevisionResolver.toRevisionContent(
                content.getBytes(StandardCharsets.UTF_8), MediaTypeUtil.TEXT_JAVASCRIPT_UTF8);
    }

    @Cacheable(value = CacheConstants.STATIC, key = "'" + Constants.CSS_ALL_DOT_CSS + "'")
    public RevisionContent getAllCss() {
        log.info("building {}", Constants.CSS_ALL_DOT_CSS);

        List<ResourceFile> cssList = Stream
                .of("/css/lib/normalize.css", "/css/style.css")
                .map(this::getStaticResourceFile)
                .collect(Collectors.toList());

        StringBuilder build = STRING_BUILDER_HOLDER.get();

        // 我们的 css
        cssList.forEach(css ->
                build
                        .append(css.getContentAsString())
                        .append("\n"));

        // 代码块高亮
        if (codeBlock.isHighlightingEnabled()) {
            build
                    .append(IOUtil.resourceToString(getHighlightJsCssPath()))
                    .append("\n");
        }

        byte[] bytes = build.toString().getBytes(StandardCharsets.UTF_8);
        return mediaRevisionResolver.toRevisionContent(bytes, MediaTypeUtil.TEXT_CSS_UTF8);
    }

    @Cacheable(value = CacheConstants.STATIC, key = "'" + Constants.HELP + "'")
    public ContentHtml getHelpContent() {
        log.info("building {}", Constants.HELP);
        ResourceFile help = getStaticResourceFile("/help.adoc");
        return contentHtmlConverter.convert("Help", help.getContentAsString());
    }

    private String getLineNumberJs() {
        StringBuilder stringBuilder = new StringBuilder(7000);
        ResourceFile js = getStaticResourceFile("/js/lib/highlightjs-line-numbers.js");
        // 不依赖 highlight.js
        return stringBuilder.append("if(!window.hljs) window.hljs = {};\n")
                .append(js.getContentAsString()).append("\n")
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
                getStaticResourceFile("/js/lib/google-analytics.js").getContentAsString();
    }

    private ResourceFile getStaticResourceFile(String path) {
        return resourceFileManager.getResourceFile(resourceFileManager.getStaticRoot() + path);
    }

}
