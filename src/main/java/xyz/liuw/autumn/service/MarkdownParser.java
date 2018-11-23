package xyz.liuw.autumn.service;

import org.commonmark.Extension;
import org.commonmark.ext.autolink.AutolinkExtension;
import org.commonmark.ext.gfm.strikethrough.StrikethroughExtension;
import org.commonmark.ext.gfm.tables.TablesExtension;
import org.commonmark.ext.heading.anchor.HeadingAnchorExtension;
import org.commonmark.ext.ins.InsExtension;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

/**
 * @author liuwei
 * Created by liuwei on 2018/11/19.
 */
@Component
public class MarkdownParser {

    private Parser parser;
    private HtmlRenderer renderer;

    public MarkdownParser() {
        List<Extension> extensions = Arrays.asList(
                TablesExtension.create(),
                AutolinkExtension.create(),
                StrikethroughExtension.create(),
                HeadingAnchorExtension.create(),
                InsExtension.create());
        parser = Parser.builder()
                .extensions(extensions)
                .build();
        renderer = HtmlRenderer.builder()
                .extensions(extensions)
                .build();
    }

    public String render(String input) {
        Node document = parser.parse(input);
        return renderer.render(document);
    }
}
