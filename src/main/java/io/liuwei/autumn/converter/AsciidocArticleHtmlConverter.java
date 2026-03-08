package io.liuwei.autumn.converter;

import io.liuwei.autumn.config.AppProperties;
import io.liuwei.autumn.model.ArticleHtml;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.apache.commons.text.StringEscapeUtils;
import org.asciidoctor.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Component;

import static io.liuwei.autumn.enums.CodeBlockHighlighterEnum.*;

/**
 * @author liuwei
 * @since 2020-06-01 18:45
 */
@Component
@RequiredArgsConstructor
public class AsciidocArticleHtmlConverter implements ArticleHtmlConverter {

    private final Asciidoctor asciidoctor;

    private final AppProperties appProperties;

    private Options options;

    @PostConstruct
    public void init() {

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
    public ArticleHtml convert(String title, String content) {
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
        Document document = Jsoup.parse(contentHtml);
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
