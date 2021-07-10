package io.liuwei.autumn.config;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

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

    private CodeBlock codeBlock = new CodeBlock();

    @Getter
    @Setter
    public static class CodeBlock {
        private boolean highlightingEnabled = true;

        private String highlightingStyle = "default";

        private List<String> highlightingLanguages = Arrays.asList(
                "bash,clojure,css,java,javascript,json,lisp,lua,nginx,php,python,ruby,sql,xml,yaml"
                        .split(","));

        /**
         * Maven org.webjars:highlightjs version
         */
        private String highlightjsVersion;

        private boolean lineNumberEnabled = false;

    }

}
