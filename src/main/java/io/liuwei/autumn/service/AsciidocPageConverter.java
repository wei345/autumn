package io.liuwei.autumn.service;

import io.liuwei.autumn.data.Page;
import org.asciidoctor.Asciidoctor;
import org.asciidoctor.OptionsBuilder;
import org.asciidoctor.jruby.internal.JRubyAsciidoctor;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

/**
 * @author liuwei
 * @since 2020-06-01 18:45
 */
public class AsciidocPageConverter implements PageConverter {

    @Override
    public Page.PageHtml convert(String title, String body, String path) {
        Asciidoctor asciidoctor = new JRubyAsciidoctor();
        String html = asciidoctor.convert(body, OptionsBuilder.options().headerFooter(true));
        Document document = Jsoup.parse(html);
        html = document.body().html(); // pretty print HTML
        return new Page.PageHtml(null, title, html);
    }
}
