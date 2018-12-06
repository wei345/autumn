package xyz.liuw.autumn.service;
/*
import com.vip.vjtools.vjkit.text.StringBuilderHolder;
import org.commonmark.Extension;
import org.commonmark.ext.autolink.AutolinkExtension;
import org.commonmark.ext.gfm.strikethrough.StrikethroughExtension;
import org.commonmark.ext.gfm.tables.TablesExtension;
import org.commonmark.ext.heading.anchor.HeadingAnchorExtension;
import org.commonmark.ext.ins.InsExtension;
import org.commonmark.node.*;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.AttributeProvider;
import org.commonmark.renderer.html.HtmlRenderer;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

*//**
 * @author liuwei
 * Created by liuwei on 2018/11/19.
 *//*
public class CommonmarkMarkdownParser implements MarkdownParser {

    private Parser parser;
    private HtmlRenderer renderer;
    private StringBuilderHolder stringBuilderHolder = new StringBuilderHolder(1024);

    public CommonmarkMarkdownParser() {
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

    @Override
    public String render(String path, String title, String body) {
        return render(title, body);
    }

    @Override
    public String render(String title, String body) {
        return render(stringBuilderHolder.get()
                .append("# ")
                .append(title)
                .append("\n")
                .append(body)
                .toString());
    }

    @Override
    public String render(String input) {
        Node document = parser.parse(input);
//        document.accept(new TocVisitor());
        return renderer.render(document);
    }

    *//**
     * Not thread-safe
     *//*
    static class TocVisitor extends AbstractVisitor {

        private StringBuilderHolder stringBuilderHolder = new StringBuilderHolder(1024);
        private StringBuilder stringBuilder = stringBuilderHolder.get();

        @Override
        public void visit(Document document) {
            Node node = document.getFirstChild();
            while (node != null) {
                if (node instanceof Heading) {
                    Heading heading = (Heading) node;
                    stringBuilder.append("<h").append(heading.getLevel()).append(">");
                    heading.accept(this);
                    stringBuilder.append("</").append(heading.getLevel()).append(">\n");
                }
                node = node.getNext();
            }
            System.out.println(stringBuilder.toString());
        }

        @Override
        public void visit(Text text) {
            stringBuilder.append(text.getLiteral());
        }
    }

    class ImageAttributeProvider implements AttributeProvider {
        @Override
        public void setAttributes(Node node, String tagName, Map<String, String> attributes) {
            if (node instanceof Image) {
                attributes.put("class", "border");
            }
        }
    }
}*/
