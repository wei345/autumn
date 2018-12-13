package xyz.liuw.autumn.service;

import com.vip.vjtools.vjkit.io.FileUtil;
import com.vladsch.flexmark.ast.Node;
import com.vladsch.flexmark.ext.autolink.AutolinkExtension;
import com.vladsch.flexmark.ext.gfm.strikethrough.StrikethroughExtension;
import com.vladsch.flexmark.ext.tables.TablesExtension;
import com.vladsch.flexmark.ext.toc.TocExtension;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.options.MutableDataSet;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

/**
 * @author liuwei
 * Created by liuwei on 2018/12/8.
 */
public class FlexmarkMarkdownParserTest {

    @Test
    public void flexmark() throws IOException {
        MutableDataSet options = new MutableDataSet();

        // uncomment to set optional extensions
        options.set(Parser.EXTENSIONS, Arrays.asList(
                TablesExtension.create(),
                StrikethroughExtension.create(),
                AutolinkExtension.create(),
                TocExtension.create()))
                .set(TocExtension.LEVELS, 255)
                .set(TocExtension.TITLE, "Table of Contents")
                .set(TocExtension.DIV_CLASS, "toc");

        // uncomment to convert soft-breaks to hard breaks
        //options.set(HtmlRenderer.SOFT_BREAK, "<br />\n");

        Parser parser = Parser.builder(options).build();
        HtmlRenderer renderer = HtmlRenderer.builder(options).build();

        String source = "[TOC]\n" + FileUtil.toString(new File("../notes/java/intellijidea.md"));


        // You can re-use parser and renderer instances
        Node document = parser.parse(source);
        String html = renderer.render(document);  // "<p>This is <em>Sparta</em></p>\n"
        System.out.println(html);
    }

    @Test
    public void render() throws IOException {

        String source = "[TOC]\n" + FileUtil.toString(new File("../notes/java/concurrent/thread-life-cycle.md"));

        FlexmarkMarkdownParser parser = new FlexmarkMarkdownParser();
        System.out.println(parser.render(source));

    }
}