package xyz.liuw.autumn.service;

import com.vip.vjtools.vjkit.text.StringBuilderHolder;
import com.vladsch.flexmark.ast.Node;
import com.vladsch.flexmark.ext.anchorlink.AnchorLinkExtension;
import com.vladsch.flexmark.ext.autolink.AutolinkExtension;
import com.vladsch.flexmark.ext.gfm.strikethrough.StrikethroughExtension;
import com.vladsch.flexmark.ext.tables.TablesExtension;
import com.vladsch.flexmark.ext.toc.TocExtension;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.options.MutableDataSet;
import org.apache.commons.lang3.StringUtils;
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
                // @formatter:off
                // 顶层元素 div.toc
                .set(TocExtension.DIV_CLASS, "toc")
                // <h3>title</h3>
                .set(TocExtension.TITLE_LEVEL, 3)
                .set(TocExtension.TITLE, "Table of Contents");
                // 或者顶层元素 ul.toc
                // .set(TocExtension.LIST_CLASS, "toc");
                // @formatter:on

        parser = Parser.builder(options).build();
        renderer = HtmlRenderer.builder(options).build();
    }

    @Override
    public String render(String path, String title, String body) {
        StringBuilder stringBuilder = stringBuilderHolder.get();
        // TOC
        stringBuilder.append("[TOC]\n");
        // 标题
        stringBuilder.append("# ");
        if (StringUtils.isNotBlank(path)) {
            stringBuilder.append("[")
                    .append(title)
                    .append("](")
                    .append(path)
                    .append(")");
        } else {
            stringBuilder.append(title);
        }
        // body
        stringBuilder.append("\n").append(body);
        return render(stringBuilder.toString());
    }

    @Override
    public String render(String title, String body) {
        return render(null, title, body);
    }

    @Override
    public String render(String source) {
        Node document = parser.parse(source);
        return renderer.render(document);
    }


}
