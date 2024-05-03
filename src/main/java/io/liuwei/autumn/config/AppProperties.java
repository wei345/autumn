package io.liuwei.autumn.config;

import io.liuwei.autumn.enums.CodeBlockHighlighterEnum;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.unit.DataSize;

import java.util.Arrays;
import java.util.List;

/**
 * @author liuwei
 * @since 2021-07-07 19:57
 */
@Data
@ConfigurationProperties(prefix = "autumn")
public class AppProperties {

    private String siteTitle = "Autumn";

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
    }

    @Data
    public static class CodeBlock {
        private CodeBlockHighlighterEnum highlighter = CodeBlockHighlighterEnum.ROUGE;

        private Highlightjs highlightjs = new Highlightjs();

        @Data
        public static class Highlightjs {
            private String theme = "default";

            private List<String> languages = Arrays.asList(
                    "bash,clojure,css,java,javascript,json,lisp,lua,nginx,php,python,ruby,sql,xml,yaml".split(","));

            /**
             * Maven org.webjars:highlightjs version
             */
            private String version;

            private boolean lineNumberEnabled = false;
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
        private volatile Integer level;
    }

}
