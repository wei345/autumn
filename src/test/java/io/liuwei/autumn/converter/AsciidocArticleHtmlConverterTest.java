package io.liuwei.autumn.converter;

import io.liuwei.autumn.model.ArticleHtml;
import org.apache.commons.io.IOUtils;
import org.asciidoctor.Asciidoctor;
import org.asciidoctor.AttributesBuilder;
import org.asciidoctor.OptionsBuilder;
import org.asciidoctor.jruby.internal.JRubyAsciidoctor;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * @author liuwei
 * @since 2020-06-01 18:57
 */
public class AsciidocArticleHtmlConverterTest {

    @Test
    public void convert() throws IOException {
        String adoc = IOUtils.resourceToString("/example.adoc", StandardCharsets.UTF_8);

        ArticleHtml articleHtml = convert("title", adoc, "/example.adoc");

        System.out.println(articleHtml.getContentHtml());
    }

    public ArticleHtml convert(String title, String body, String path) {
        Asciidoctor asciidoctor = new JRubyAsciidoctor();

        String bodyHtml = asciidoctor.convert(body,
                OptionsBuilder.options()
                        .attributes(AttributesBuilder.attributes().showTitle(true).tableOfContents(true)));

        Document document = Jsoup.parse(bodyHtml);
        bodyHtml = document.body().html(); // pretty print HTML
        return new ArticleHtml(title, null, null, bodyHtml);
    }

}