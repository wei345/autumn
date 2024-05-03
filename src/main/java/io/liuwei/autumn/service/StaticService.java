package io.liuwei.autumn.service;

import com.vip.vjtools.vjkit.text.StringBuilderHolder;
import io.liuwei.autumn.component.AsciidocArticleParser;
import io.liuwei.autumn.config.AppProperties;
import io.liuwei.autumn.constant.CacheKeys;
import io.liuwei.autumn.constant.CacheNames;
import io.liuwei.autumn.constant.Constants;
import io.liuwei.autumn.converter.ArticleHtmlConverter;
import io.liuwei.autumn.manager.ResourceFileManager;
import io.liuwei.autumn.manager.RevisionContentManager;
import io.liuwei.autumn.model.ArticleHtml;
import io.liuwei.autumn.model.ResourceFile;
import io.liuwei.autumn.model.RevisionContent;
import io.liuwei.autumn.util.HtmlUtil;
import io.liuwei.autumn.util.IOUtil;
import io.liuwei.autumn.util.JsCompressor;
import io.liuwei.autumn.util.MediaTypeUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.liuwei.autumn.enums.CodeBlockHighlighterEnum.HIGHLIGHTJS;
import static io.liuwei.autumn.enums.CodeBlockHighlighterEnum.ROUGE;


/**
 * @author liuwei
 * Created by liuwei on 2018/11/19.
 */
@SuppressWarnings("UnusedReturnValue")
@Component
@Slf4j
@RequiredArgsConstructor
public class StaticService {
    private static final StringBuilderHolder STRING_BUILDER_HOLDER = new StringBuilderHolder(1024);

    private final ResourceFileManager resourceFileManager;

    private final RevisionContentManager revisionContentManager;

    private final ArticleHtmlConverter articleHtmlConverter;

    private final AsciidocArticleParser asciidocArticleParser;

    private final AppProperties.StaticResource staticResource;

    private final AppProperties.Analytics analytics;

    private final AppProperties.CodeBlock codeBlock;

    @Cacheable(value = CacheNames.STATIC, key = CacheKeys.JS_ALL_DOT_JS)
    public RevisionContent getAllJs() {
        log.info("building {}", Constants.JS_ALL_DOT_JS);

        StringBuilder builder = STRING_BUILDER_HOLDER.get();

        // 代码块高亮
        if (codeBlock.getHighlighter() == HIGHLIGHTJS) {
            builder.append(getHighlightJs()).append("\n");
            if (codeBlock.getHighlightjs().isLineNumberEnabled())
                builder.append(getLineNumberJs()).append("\n");
        }

        // Google 分析
        if (StringUtils.isNotBlank(analytics.getGoogleAnalyticsId())) {
            builder
                    .append(getGoogleAnalyticsJs())
                    .append("\n");
        }
        if (StringUtils.isNotBlank(analytics.getGoogleAnalytics4MeasurementId())) {
            builder
                    .append(getGoogleAnalytics4Js())
                    .append("\n");
        }

        // 我们的 js，包到一个 function 里
        builder.append("\"use strict\";\n");
        builder.append("(function () {\n");
        List<ResourceFile> jsList = Stream
                .of("/js/script.js", "/js/quick_search.js", "/js/util.js")
                .map(this::getStaticResourceFile)
                .collect(Collectors.toList());
        jsList.forEach(js ->
                builder.append(
                                js.getContentAsString()
                                        .replaceFirst("\"use strict\";\n", "")
                                        .trim())
                        .append("\n"));
        builder.append("})();\n");

        // 压缩
        String content = builder.toString();
        if (staticResource.isJsCompressionEnabled()) {
            String depend = "var autumn = {ctx: '', prefix: '', treeVersionKeyValue: ''}";
            content = JsCompressor.compressJs(depend, content);
        }

        return revisionContentManager.toRevisionContent(
                content.getBytes(StandardCharsets.UTF_8), MediaTypeUtil.TEXT_JAVASCRIPT_UTF8);
    }

    @Cacheable(value = CacheNames.STATIC, key = CacheKeys.CSS_ALL_DOT_CSS)
    public RevisionContent getAllCss() {
        log.info("building {}", Constants.CSS_ALL_DOT_CSS);
        StringBuilder build = STRING_BUILDER_HOLDER.get();

        if (codeBlock.getHighlighter() == HIGHLIGHTJS)
            build.append(IOUtil.resourceToString(getHighlightJsCssPath())).append("\n");

        List<String> cssFiles = new ArrayList<>();
        cssFiles.add("/css/lib/asciidoctor.css");
        if (codeBlock.getHighlighter() == ROUGE)
            cssFiles.add("/css/lib/rouge-solarized.css");
        cssFiles.add("/css/style.css");

        cssFiles.stream()
                .map(this::getStaticResourceFile)
                .forEach(css ->
                        build.append(css.getContentAsString()).append("\n"));

        byte[] bytes = build.toString().getBytes(StandardCharsets.UTF_8);
        return revisionContentManager.toRevisionContent(bytes, MediaTypeUtil.TEXT_CSS_UTF8);
    }

    @Cacheable(value = CacheNames.STATIC, key = CacheKeys.HELP)
    public ArticleHtml getHelpArticleHtml() {
        log.info("building {}", Constants.HELP);
        ResourceFile help = getStaticResourceFile("/help.adoc");
        String content = asciidocArticleParser.parse(help.getContentAsString(), "/help").getContent();
        ArticleHtml articleHtml = articleHtmlConverter.convert("Help", content);
        articleHtml.setContentHtml(HtmlUtil.addHeadingClass(articleHtml.getContentHtml()));
        return articleHtml;
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
                .getHighlightjs().getLanguages()
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
        return getHighlightJsBasePath() + "/styles/" + codeBlock.getHighlightjs().getTheme() + ".css";
    }

    private String getHighlightJsBasePath() {
        return "/META-INF/resources/webjars/highlightjs/" + codeBlock.getHighlightjs().getVersion();
    }

    private String getGoogleAnalyticsJs() {
        return "window['GoogleAnalyticsObject'] = 'ga'; window.ga = {" +
                "q: [['create', '" + analytics.getGoogleAnalyticsId() + "', 'auto'], ['send', 'pageview']], " +
                "l: 1 * new Date()};\n" +
                getStaticResourceFile("/js/lib/google-analytics.js").getContentAsString();
    }

    private String getGoogleAnalytics4Js() {
        String measurementId = analytics.getGoogleAnalytics4MeasurementId();
        return "(function(){\n" +
                "  var script = document.createElement('script');\n" +
                "  script.async = true;\n" +
                "  script.src = 'https://www.googletagmanager.com/gtag/js?id=" + measurementId + "'\n" +
                "  document.getElementsByTagName('head')[0].append(script);\n" +
                "  \n" +
                "  window.dataLayer = window.dataLayer || [];\n" +
                "  function gtag(){dataLayer.push(arguments);}\n" +
                "  gtag('js', new Date());\n" +
                "  gtag('config', '" + measurementId + "');\n" +
                "})();";
    }

    private ResourceFile getStaticResourceFile(String path) {
        return resourceFileManager.getResourceFile(resourceFileManager.getStaticRoot() + path);
    }

}
