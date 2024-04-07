package io.liuwei.autumn.converter;

import io.liuwei.autumn.config.AppProperties;
import io.liuwei.autumn.model.ArticleHtml;
import lombok.RequiredArgsConstructor;
import org.apache.commons.text.StringEscapeUtils;
import org.asciidoctor.Asciidoctor;
import org.asciidoctor.AttributesBuilder;
import org.asciidoctor.OptionsBuilder;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

/**
 * @author liuwei
 * @since 2020-06-01 18:45
 */
@Component
@RequiredArgsConstructor
public class AsciidocArticleHtmlConverter implements ArticleHtmlConverter {

    private final Asciidoctor asciidoctor;

    private final AppProperties appProperties;

    private OptionsBuilder optionsBuilder;

    @PostConstruct
    public void init() {
        AttributesBuilder ab = AttributesBuilder
                .attributes()
                .showTitle(true)
                .setAnchors(true);
        if (appProperties.getToc().isEnabled())
            ab.tableOfContents(true);
        if (appProperties.getToc().getLevel() != null)
            ab.attribute("toclevels",
                    appProperties.getToc().getLevel());
        this.optionsBuilder = OptionsBuilder.options().attributes(ab);
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
        String contentHtml = asciidoctor.convert(content, optionsBuilder);

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
                            .text("TOC"));

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
