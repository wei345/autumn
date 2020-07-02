package io.liuwei.autumn.converter;

import com.vip.vjtools.vjkit.text.EscapeUtil;
import io.liuwei.autumn.domain.Page;
import io.liuwei.autumn.service.DataService;
import io.liuwei.autumn.util.Asciidoctors;
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
public class AsciidocPageConverter extends AbstractPageConverter {

    private final OptionsBuilder optionsBuilder = OptionsBuilder.options()
            .attributes(AttributesBuilder.attributes()
                    .showTitle(true)
                    .tableOfContents(true));

    public AsciidocPageConverter(DataService dataService) {
        super(dataService);
    }

    @Override
    protected Page.PageHtml parse(String title, String body) {

        String titleId = "articletitle";

        String titleHtml = "<h1 id=\"" + titleId + "\">" + EscapeUtil.escapeHtml(title) + "</h1>";

        // body
        String bodyHtml = Asciidoctors.getAsciidoctor().convert(body, optionsBuilder);

        Document document = Jsoup.parse(bodyHtml);
        Element toc = document.getElementById("toc");
        toc.remove();
        bodyHtml = document.body().html();

        // toc
        // change toc title div to h3
        toc.select("#toctitle").first()
                .replaceWith(new Element("h3")
                        .attr("id", "toctitle")
                        .text("TOC"));

        // add article title in toc
        Element oldUl = toc.selectFirst("ul.sectlevel1");

        Element a = new Element("a").attr("href", "#" + titleId).text(title);
        Element li = new Element("li").appendChild(a).appendChild(oldUl);
        Element newUl = new Element("ul").attr("class", "sectlevel0").appendChild(li);
        toc.appendChild(newUl);
        String tocHtml = toc.outerHtml();

        return new Page.PageHtml(tocHtml, titleHtml, bodyHtml);
    }
}
