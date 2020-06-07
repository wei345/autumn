package io.liuwei.autumn.service;

import io.liuwei.autumn.data.Page;
import org.apache.commons.io.IOUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * @author liuwei
 * @since 2020-06-01 18:57
 */
public class AsciidocPageConverterTest {

    private final AsciidocPageConverter converter = new AsciidocPageConverter();

    @Test
    public void convert() throws IOException {
        String adoc = IOUtils.resourceToString("/example.adoc", StandardCharsets.UTF_8);

        Page.PageHtml pageHtml = converter.convert("title", adoc, "/example.adoc");

        System.out.println(pageHtml.getContent());
    }
}