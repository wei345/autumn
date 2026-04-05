package io.liuwei.autumn.parser;

import io.liuwei.autumn.config.AppProperties;
import io.liuwei.autumn.model.Article;
import io.liuwei.autumn.util.LineReader;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.asciidoctor.Asciidoctor;
import org.asciidoctor.Attributes;
import org.asciidoctor.Options;
import org.asciidoctor.SafeMode;
import org.asciidoctor.ast.Document;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Map;

import static io.liuwei.autumn.enums.CodeBlockHighlighterEnum.*;
import static org.apache.commons.lang3.StringUtils.*;

/**
 * @author liuwei
 * @since 2021-07-07 17:21
 */
@Component
@RequiredArgsConstructor
public class AsciidocArticleParser extends AbstractArticleParser {
    private static final String ATTR_PREFIX = ":";
    private static final String TITLE_PREFIX = "= ";

    private final Asciidoctor asciidoctor;
    private final AppProperties appProperties;
    private Options options;

    @PostConstruct
    void init() {
        Attributes attributes = Attributes.builder()
                .showTitle(true)
                .allowUriRead(true) // Replaced .setAnchors(true) with modern equivalent if needed,
                // though 'anchors' are often enabled by default now.
                .sectionNumbers(true)
                .sourceHighlighter(appProperties.getCodeBlock().getHighlighter() == ROUGE ? "rouge" : null)
                // Handle TOC logic
                .tableOfContents(appProperties.getToc().isEnabled())
                .attribute("toclevels", appProperties.getToc().getLevel())
                // Handle Table Stripes
                .attribute("table-stripes", appProperties.getTableStripes() != null ?
                        appProperties.getTableStripes().name().toLowerCase() : null)
                // LaTeX Support
                .attribute("stem", "latexmath")
                .build();

        this.options = Options.builder()
                .attributes(attributes)
                .safe(SafeMode.SAFE) // Required in modern AsciidoctorJ for security
                .build();
    }

    @Override
    protected void parseSource(Article article) {
        String source = article.getSource();
        LineReader lineReader = new LineReader(source);

        parseTitle(lineReader, article);
        parseAttributes(lineReader, article);
        parseBody(lineReader, article);
    }

    private void parseTitle(LineReader lineReader, Article article) {
        article.setTitle(parseTitle(lineReader, TITLE_PREFIX));
    }

    private void parseAttributes(LineReader lineReader, Article article) {
        Map<String, Object> attributes = Collections.emptyMap();

        String attrText = lineReader.nextLinesAsString(line ->
                        isBlank(line) || line.startsWith(ATTR_PREFIX))
                .trim();

        if (isNotBlank(attrText)) {

            Options options = Options.builder()
                    .parseHeaderOnly(true)
                    .safe(SafeMode.SAFE)
                    .build();

            Document doc = asciidoctor.load(attrText, options);
            attributes = doc.getAttributes();
        }
        setAttributes(attributes, article);
    }

    private void parseBody(LineReader lineReader, Article article) {
        article.setBody(lineReader.remainingText().trim());
    }

    @Override
    protected org.jsoup.nodes.Document renderBodyAsDocument(Article article) {

        String body = article.getBody();

        String bodyHtml = asciidoctor.convert(body, options);

        org.jsoup.nodes.Document bodyDoc = Jsoup.parse(bodyHtml);

        Element tocEl = bodyDoc.getElementById("toc");
        if (tocEl != null) {
            // 把 toc 标题标签改为 h3
            tocEl
                    .select("#toctitle")
                    .first()
                    .replaceWith(new Element("h3")
                            .attr("id", "toctitle")
                            .text(appProperties.getToc().getTitle()));

        }

        return bodyDoc;
    }
}
