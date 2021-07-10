package io.liuwei.autumn.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author liuwei
 * @since 2021-07-07 19:57
 */
@Data
@ConfigurationProperties(prefix = "autumn")
public class AppProperties {

    private String siteTitle;

    private CodeBlock codeBlock = new CodeBlock();

    @Data
    public static class CodeBlock {
        private boolean highlightingEnabled = true;

        private String highlightingStyle = "default";

        private String highlightingLanguages = "bash,clojure,css,java,javascript,json,lisp,lua,nginx,php,python,ruby,sql,xml,yaml";

        /**
         * Maven org.webjars:highlightjs version
         */
        private String highlightjsVersion;

        private boolean lineNumberEnabled = false;

    }
}
