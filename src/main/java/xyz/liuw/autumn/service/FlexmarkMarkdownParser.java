package xyz.liuw.autumn.service;

import com.vip.vjtools.vjkit.text.StringBuilderHolder;
import com.vladsch.flexmark.ast.Node;
import com.vladsch.flexmark.ext.autolink.AutolinkExtension;
import com.vladsch.flexmark.ext.gfm.strikethrough.StrikethroughExtension;
import com.vladsch.flexmark.ext.tables.TablesExtension;
import com.vladsch.flexmark.ext.toc.TocExtension;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.options.MutableDataSet;
import org.springframework.stereotype.Component;

import java.util.Arrays;

/**
 * @author liuwei
 * Created by liuwei on 2018/11/30.
 */
@Component
public class FlexmarkMarkdownParser implements MarkdownParser {

    private Parser parser;
    private HtmlRenderer renderer;
    private StringBuilderHolder stringBuilderHolder = new StringBuilderHolder(1024);

    public FlexmarkMarkdownParser() {
        MutableDataSet options = new MutableDataSet()
                .set(Parser.EXTENSIONS, Arrays.asList(
                        TablesExtension.create(),
                        StrikethroughExtension.create(),
                        AutolinkExtension.create(),
                        TocExtension.create()))
                .set(TocExtension.LEVELS, 127)
//                .set(TocExtension.TITLE, "Table of Contents")
                .set(TocExtension.LIST_CLASS, "toc");

        parser = Parser.builder(options).build();
        renderer = HtmlRenderer.builder(options).build();
    }

    @Override
    public String render(String title, String body) {
        return render(stringBuilderHolder.get()
                .append("[TOC]\n")
                .append("# ")
                .append(title)
                .append("\n")
                .append(body)
                .toString());
    }

    @Override
    public String render(String source) {
        Node document = parser.parse(source);
        return renderer.render(document);
    }


}
