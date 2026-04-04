package io.liuwei.autumn.parser;

import io.liuwei.autumn.config.AppProperties;
import io.liuwei.autumn.model.Article;
import io.liuwei.autumn.model.ArticleHtml;
import io.liuwei.autumn.util.LineReader;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.asciidoctor.Asciidoctor;
import org.asciidoctor.Attributes;
import org.asciidoctor.Options;
import org.asciidoctor.SafeMode;
import org.asciidoctor.ast.Document;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static io.liuwei.autumn.enums.CodeBlockHighlighterEnum.*;

/**
 * @author liuwei
 * @since 2021-07-07 17:21
 */
@Component
@SuppressWarnings("FieldCanBeLocal")
@RequiredArgsConstructor
public class AsciidocArticleParser extends AbstractArticleParser {
    private final Asciidoctor asciidoctor;
    private final AppProperties appProperties;

    private final String attrPrefix = ":";
    private final String titlePrefix = "= ";
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
        // Use the builder to load only the header (fast and modern)
        Options options = Options.builder()
                .parseHeaderOnly(true)
                .safe(SafeMode.SAFE)
                .build();

        String source = article.getSource();
        LineReader lineReader = new LineReader(source);

        parseTitle(lineReader, article);
        parseAttributes(lineReader, article);
        parseBody(lineReader, article);

    }

    private void parseAttributes(LineReader lineReader, Article article) {
        List<String> lines = new ArrayList<>();
        for (String line : lineReader) {
            if (StringUtils.isBlank(line)) continue;
            if (line.startsWith(":"))
                lines.add(line);
            else {
                lineReader.back();
                break;
            }
        }
        String s = StringUtils.join(lines);
        Document document = asciidoctor.load(s, options);
        Map<String, Object> attributes = document.getAttributes();
        setAttributes(attributes, article);
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

    @Override
    public ArticleHtml toHtml(Article article) {
        String title = article.getTitle();
        String content = article.getBody();
        // title html
        String titleId = "article-title";
        String titleHtml = "<h1 id=\"" + titleId + "\" class=\"heading\">" +
                StringEscapeUtils.escapeHtml4(title) +
                "<a class=\"anchor\" href=\"\"></a>" +
                "</h1>";

        // content html
        String contentHtml = asciidoctor.convert(content, options);

        // toc html
        String tocHtml = null;
        org.jsoup.nodes.Document document = Jsoup.parse(contentHtml);
        Element tocEl = document.getElementById("toc");
        if (tocEl != null) {
            // 从 content html 中删除 toc
            tocEl.remove();
            contentHtml = document.body().html();

            // 把 toc 标题标签改为 h3
            tocEl
                    .select("#toctitle")
                    .first()
                    .replaceWith(new Element("h3")
                            .attr("id", "toctitle")
                            .text(appProperties.getToc().getTitle()));

            // 在 toc 第一行插入文章标题，点击可以跳到标题
            Element oldUl = tocEl.selectFirst("ul.sectlevel1");
            Element articleTitleLink = new Element("a")
                    .attr("href", "#" + titleId)
                    .text(title);
            Element articleTitleLi = new Element("li")
                    .appendChild(articleTitleLink)
                    .appendChild(oldUl);
            Element newUl = new Element("ul")
                    .attr("class", "sectlevel0")
                    .appendChild(articleTitleLi);
            tocEl.appendChild(newUl);

            tocHtml = tocEl.outerHtml();
        }

        return new ArticleHtml(title, titleHtml, tocHtml, contentHtml);
    }
}
