package io.liuwei.autumn.converter;

import com.vip.vjtools.vjkit.text.EscapeUtil;
import io.liuwei.autumn.model.ContentHtml;
import org.asciidoctor.Asciidoctor;
import org.asciidoctor.AttributesBuilder;
import org.asciidoctor.OptionsBuilder;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Component;

/**
 * @author liuwei
 * @since 2020-06-01 18:45
 */
@Component
public class AsciidocContentHtmlConverter implements ContentHtmlConverter {

    private final OptionsBuilder optionsBuilder = OptionsBuilder.options()
            .attributes(AttributesBuilder.attributes()
                    .showTitle(true)
                    .tableOfContents(true));

    private Asciidoctor asciidoctor;

    public AsciidocContentHtmlConverter(Asciidoctor asciidoctor) {
        this.asciidoctor = asciidoctor;
    }

    @Override
    public ContentHtml convert(String title, String body) {
        // title html
        String titleId = "articletitle";
        String titleHtml = "<h1 id=\"" + titleId + "\">" + EscapeUtil.escapeHtml(title) + "</h1>";

        // content html
        String contentHtml = asciidoctor.convert(body, optionsBuilder);

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

        return new ContentHtml(title, titleHtml, tocHtml, contentHtml);
    }
}
