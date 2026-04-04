package io.liuwei.autumn.config;

import io.liuwei.autumn.enums.CodeBlockHighlighterEnum;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.unit.DataSize;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;

/**
 * @author liuwei
 * @since 2021-07-07 19:57
 */
@Data
@ConfigurationProperties(prefix = "autumn")
public class AppProperties {

    private final Access access = new Access();
    private final RememberMe rememberMe = new RememberMe();
    private final SiteData data = new SiteData();
    private final StaticResource staticResource = new StaticResource();
    private final Cache cache = new Cache();
    private final CodeBlock codeBlock = new CodeBlock();
    private final Breadcrumb breadcrumb = new Breadcrumb();
    private final Search search = new Search();
    private final Analytics analytics = new Analytics();
    private final Toc toc = new Toc();
    private final Clipboard clipboard = new Clipboard();
    private String siteTitle = "Autumn";
    /**
     * Supported placeholders
     * <ul>
     *     <li><i>{year}</i> will be replaced with four-digit year, e.g. 2025</li>
     * </ul>
     */
    private String copyrightTemplate;
    private TableStripe tableStripes;
    private String mathJaxVersion;

    public enum TableStripe {
        none, even, odd, all, hover
    }

    @Data
    public static class Access {
        /**
         * 格式：id username password salt
         * 可以用 io.liuwei.autumn.service.UserServiceTest.generateUser 生成用户名和密码。
         */
        private List<String> users;

        /**
         * 网站所有者用户 ID，有最高访问权限，可以看到 owner 级别的文章
         */
        private Long ownerUserId;
    }

    @Data
    public static class RememberMe {
        /**
         * AES key，可以用 io.liuwei.autumn.AesTest.generateKey 生成。
         */
        private String aesKey;
    }

    @Data
    public static class SiteData {
        /**
         * 数据文件目录
         * <p>
         * e.g. src/test/data
         */
        private String dir;

        /**
         * 排除的目录或文件名. e.g. /a/b,/a/c.adoc
         */
        private List<String> excludes;

        /**
         * 数据刷新间隔，0 表示禁用
         * <p>
         * 还可以通过 HTTP 接口触发数据刷新，例如 curl -s -X POST http://localhost:8601/data/reload
         */
        private Integer reloadIntervalSeconds;

        public boolean isExcluded(String filepath) {
            return excludes.stream().anyMatch(filepath::matches);
        }
    }

    @Data
    public static class StaticResource {
        /**
         * 网站静态资源文件目录
         * <p>
         * e.g. src/test/data
         */
        private String dir;

        /**
         * 静态资源刷新间隔，0 表示禁用。
         */
        private Integer reloadIntervalSeconds;

        /**
         * 是否启用 js 压缩
         */
        private boolean jsCompressionEnabled;
    }

    @Data
    public static class Cache {
        private DataSize maxMediaSize;
        private int defaultEntries;
        private int maxHtmlViewEntries;
        private int maxMediaEntries;
        private int maxSearchHitArticleEntries;
    }

    @Data
    public static class CodeBlock {
        private final Highlightjs highlightjs = new Highlightjs();
        private CodeBlockHighlighterEnum highlighter = CodeBlockHighlighterEnum.ROUGE;

        @Data
        public static class Highlightjs {

            private String themeCss = "default.css";

            private String hljs = "highlight.js";

            private List<String> languageJs = Arrays.asList(
                    ("bash.js,clojure.js,css.js,java.js,javascript.js," +
                            "json.js,lisp.js,lua.js,nginx.js,php.js,python.js," +
                            "ruby.js,sql.js,xml.js,yaml.js").split(","));

            /**
             * Maven org.webjars:highlightjs version
             */
            private String version;

            private boolean lineNumberEnabled = false;

            /**
             * Set to true for highlightjs 9.8, set to false for highlightjs 11.11.1
             */
            private boolean registerLangFunctions = false;

            /**
             * @return The highlightjs base resource path, for example
             * /META-INF/resources/webjars/highlightjs/9.8.0
             */
            public String basePath() {
                return "/META-INF/resources/webjars/highlightjs/" + version;
            }

            /**
             * @return The highlightjs resource path, for example
             * /META-INF/resources/webjars/highlightjs/9.8.0/highlight.js
             */
            public String hljsPath() {
                return basePath() + "/" + hljs;
            }

            /**
             * @return The highlightjs css resource path, for example
             * /META-INF/resources/webjars/highlightjs/9.8.0/styles/default.css
             */
            public String cssPath() {
                return basePath() + "/styles/" + themeCss;
            }

            /**
             * @return The language resource path for <code>lang</code>, for example
             * /META-INF/resources/webjars/highlightjs/9.8.0/languages/bash.js
             */
            public String languagePath(String lang) {
                return basePath() + "/languages/" + lang;
            }
        }
    }

    @Data
    public static class Breadcrumb {
        /**
         * 是否显示面包屑
         */
        private boolean enabled;
    }

    @Data
    public static class Search {
        /**
         * 搜索结果每页条数
         */
        private Integer pageSize;
    }

    @Data
    public static class Analytics {
        /**
         * Google Universal Analytics ID
         * <p>
         * UA-xxxxxxxx-x
         */
        private String googleAnalyticsId;

        /**
         * Google Analytics 4 Measurement ID
         * <p>
         * G-xxxxxxxxxx
         */
        private String googleAnalytics4MeasurementId;
    }

    @Data
    public static class Toc {
        private volatile boolean enabled;
        /**
         * The TOC level, the default is 2 that is h3 in html
         *
         * @see <a href="https://github.com/asciidoctor/asciidoctor/blob/9302213ea29e2efa75a7b3f9124ce4bbcd738174/lib/asciidoctor/converter/html5.rb#L337">Asciidoctor Source</a>
         */
        private volatile int level = 5;

        private volatile String title = "Table of Contents";
    }

    @Data
    public static class Clipboard {
        private volatile String namespaceForRedisKeys = "clipboard";
        private volatile int digestLength = 6;
        private volatile Duration maxAge = Duration.ofDays(1);
    }

}
