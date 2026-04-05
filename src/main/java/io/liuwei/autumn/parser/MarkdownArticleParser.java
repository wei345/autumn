package io.liuwei.autumn.parser;

import com.vladsch.flexmark.ext.gitlab.GitLabExtension;
import com.vladsch.flexmark.ext.tables.TablesExtension;
import com.vladsch.flexmark.ext.toc.TocExtension;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.data.MutableDataSet;
import io.liuwei.autumn.config.AppProperties;
import io.liuwei.autumn.model.Article;
import io.liuwei.autumn.util.HtmlUtil;
import io.liuwei.autumn.util.LineReader;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.Yaml;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

import static org.apache.commons.lang3.StringUtils.*;

/**
 * @author liuwei
 * @since 2026-04-04 17:25
 */
@Component
@RequiredArgsConstructor
public class MarkdownArticleParser extends AbstractArticleParser {

    private static final String TITLE_PREFIX = "# ";
    private static final String YAML_BOUNDARY = "---";
    private static final String TITLE_ATTR = "title";

    private final AppProperties appProperties;
    private Parser parser;
    private HtmlRenderer renderer;

    @PostConstruct
    void init() {
        MutableDataSet options = new MutableDataSet();

        // Add Extensions: Tables, LaTeX (GitLab style), TOC, and Anchors
        options.set(Parser.EXTENSIONS, Arrays.asList(
                TablesExtension.create(),
                GitLabExtension.create(), // Provides Math/LaTeX support via $...$ and $$...$$
                TocExtension.create()
        ));

        // Configure TOC logic based on appProperties
        options.set(TocExtension.DIV_CLASS, "toc");
        options.set(TocExtension.LEVELS, (1 << appProperties.getToc().getLevel()) - 1); // Bitmask for levels
        options.set(TocExtension.TITLE, appProperties.getToc().getTitle());

        // Configure Table stripes logic if needed (handled via CSS classes in Flexmark)
        options.set(TablesExtension.COLUMN_SPANS, true);
        options.set(TablesExtension.APPEND_MISSING_COLUMNS, true);

        this.parser = Parser.builder(options).build();
        this.renderer = HtmlRenderer.builder(options).build();
    }

    @Override
    protected void parseSource(Article article) {
        String source = article.getSource();
        LineReader lineReader = new LineReader(source);

        parseTitle(lineReader, article);
        parseHeader(lineReader, article);
        parseBody(lineReader, article);
    }

    private void parseTitle(LineReader lineReader, Article article) {
        article.setTitle(parseTitle(lineReader, TITLE_PREFIX));
    }

    private boolean isYamlHeaderBoundary(String str) {
        return str != null && str.startsWith(YAML_BOUNDARY) && str.trim().equals(YAML_BOUNDARY);
    }

    private void parseHeader(LineReader lineReader, Article article) {
        Map<String, Object> attrs = Collections.emptyMap();

        String line = lineReader.nextNonBlankLine();
        if (isYamlHeaderBoundary(line)) {
            String attrText = lineReader.nextLinesAsStringUntil(this::isYamlHeaderBoundary);
            lineReader.nextLine(); // Consume the closing boundary
            attrs = new Yaml().load(new ByteArrayInputStream(attrText.getBytes(StandardCharsets.UTF_8)));
        } else {
            lineReader.back(); // Did not consume, return back
        }

        setAttributes(attrs, article);

        // Fix title
        if (isBlank(article.getTitle()) && attrs != null) {
            Object titleObj = attrs.getOrDefault(TITLE_ATTR, "");
            String title = String.valueOf(titleObj).trim();
            if (!title.isEmpty())
                article.setTitle(title);
        }
    }


    private void parseBody(LineReader lineReader, Article article) {
        article.setBody(lineReader.remainingText().trim());
    }

    @Override
    protected org.jsoup.nodes.Document renderBodyAsDocument(Article article) {
        String body = "[TOC]\n" + article.getBody();

        com.vladsch.flexmark.util.ast.Node documentNode = parser.parse(body);
        String bodyHtml = renderer.render(documentNode);

        Document bodyDoc = Jsoup.parseBodyFragment(bodyHtml);

        Element tocEl = bodyDoc.select("div.toc").first();
        if (tocEl != null) {
            // Add id="toc"
            tocEl.attr("id", "toc");

            // Transform TOC title to H3
            Element tocTitle = tocEl.select("h1").first();
            if (tocTitle != null) {
                tocTitle.replaceWith(new Element("h3")
                        .attr("id", "toctitle")
                        .text(appProperties.getToc().getTitle()));
            }

            HtmlUtil.addNumbersToToc(tocEl);
            HtmlUtil.addSectionNumbers(bodyDoc, tocEl);
        }
        return bodyDoc;
    }
}
