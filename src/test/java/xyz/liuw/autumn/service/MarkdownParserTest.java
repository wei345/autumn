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
 * Created by liuwei on 2018/11/30.
 */
public class MarkdownParserTest {

    public static void main(String[] args) {
        System.out.println(4 | 8);
    }

    @Test
    public void render() throws IOException {
        MarkdownParser parser = new CommonmarkMarkdownParser();

//        String source = "# 标题 abc\n内容 def\n\n## 标题 2";

        String source = FileUtil.toString(new File("/Users/liuwei/code/bitbucket/weiliu/autumn/readme.md"));

        parser.render(source);
    }

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

        String source = "[TOC]\n" + FileUtil.toString(new File("/Users/liuwei/code/bitbucket/weiliu/notes/java/intellijidea.md"));


        // You can re-use parser and renderer instances
        Node document = parser.parse(source);
        String html = renderer.render(document);  // "<p>This is <em>Sparta</em></p>\n"
        System.out.println(html);
    }
}