package io.liuwei.autumn.parser;

import com.vladsch.flexmark.ext.gitlab.GitLabExtension;
import com.vladsch.flexmark.ext.tables.TablesExtension;
import com.vladsch.flexmark.ext.toc.TocExtension;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.data.MutableDataSet;
import io.liuwei.autumn.config.AppProperties;
import io.liuwei.autumn.model.Article;
import io.liuwei.autumn.model.ArticleHtml;
import io.liuwei.autumn.util.HtmlUtil;
import io.liuwei.autumn.util.LineReader;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.Yaml;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * @author liuwei
 * @since 2026-04-04 17:25
 */
@Component
@RequiredArgsConstructor
public class MarkdownArticleParser extends AbstractArticleParser {

    private final AppProperties appProperties;
    private final Pattern YAML_HEADER = Pattern.compile("(^|\\n)---(\\s*\\n.+?\\n)---(\\n|$)", Pattern.MULTILINE);
    private final String titlePrefix = "# ";
    private final String titleAttr = "title";
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

    private void parseHeader(LineReader lineReader, Article article) {
        boolean hasBegun = false;
        List<String> lines = new ArrayList<>();
        for (String line : lineReader) {
            if (StringUtils.isBlank(line)) continue;
            if (line.equals("---")) {
                if (hasBegun) break;
                else hasBegun = true;
            }
            if (hasBegun)
                lines.add(line);
            else {
                lineReader.back();
                break;
            }
        }

        String s = StringUtils.join(lines, "\n");
        Map<String, Object> attrs = new Yaml().load(new ByteArrayInputStream(s.getBytes(StandardCharsets.UTF_8)));
        setAttributes(attrs, article);

        if (StringUtils.isBlank(article.getTitle()) && attrs != null) {
            Object titleObj = attrs.get(titleAttr);
            if (titleObj != null) {
                String title = String.valueOf(titleObj).trim();
                if (!title.isEmpty())
                    article.setTitle(title);
            }
        }
    }

    private void parseTitle(LineReader lineReader, Article article) {
        String title = "";
        for (String line : lineReader) {
            if (StringUtils.isBlank(line)) continue;
            if (line.startsWith(titlePrefix))
                title = line.substring(titlePrefix.length()).trim();
            else {
                lineReader.back();
                break;
            }
        }
        article.setTitle(title);
    }

    private void parseBody(LineReader lineReader, Article article) {
        for (String line : lineReader) {
            if (StringUtils.isNotBlank(line)) {
                lineReader.back();
                break;
            }
        }
        article.setBody(lineReader.remainingText());
    }

    public ArticleHtml toHtml(Article article) {
        String title = article.getTitle();
        String body = "[TOC]\n" + article.getBody();

        // 1. Generate Title HTML
        String titleId = "article-title";
        String titleHtml = "<h1 id=\"" + titleId + "\" class=\"heading\">" +
                StringEscapeUtils.escapeHtml4(title) +
                "<a class=\"anchor\" href=\"\"></a>" +
                "</h1>";

        // 2. Convert Content to HTML
        com.vladsch.flexmark.util.ast.Node documentNode = parser.parse(body);
        String bodyHtml = renderer.render(documentNode);

        // 3. Handle TOC Extraction and Manipulation via Jsoup
        String tocHtml = null;
        Document bodyDoc = Jsoup.parseBodyFragment(bodyHtml);
        Element tocEl = bodyDoc.select("div.toc").first();

        if (tocEl != null) {
            // Remove TOC from the main content body
            tocEl.remove();

            // Add id="toc"
            tocEl.attr("id", "toc");

            // Transform TOC title to H3
            Element tocTitle = tocEl.select("h1").first();
            if (tocTitle != null) {
                tocTitle.replaceWith(new Element("h3")
                        .attr("id", "toctitle")
                        .text(appProperties.getToc().getTitle()));
            }

            // Insert Article Title at the top of the TOC list
            Element rootUl = tocEl.select("ul").first();
            if (rootUl != null) {
                Element articleTitleLi = new Element("li")
                        .appendChild(new Element("a")
                                .attr("href", "#" + titleId)
                                .text(title));

                // Nest the original TOC under this new root
                rootUl.before(new Element("ul").attr("class", "sectlevel0").appendChild(articleTitleLi));
                articleTitleLi.appendChild(rootUl);
            }

            HtmlUtil.addNumbersToToc(tocEl);
            HtmlUtil.addSectionNumbers(bodyDoc, tocEl);

            bodyHtml = bodyDoc.html();
            tocHtml = tocEl.outerHtml();
        }

        return new ArticleHtml(title, titleHtml, tocHtml, bodyHtml);
    }
}
