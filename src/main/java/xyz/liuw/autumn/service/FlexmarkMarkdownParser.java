package xyz.liuw.autumn.service;

import com.google.common.collect.Sets;
import com.vip.vjtools.vjkit.text.StringBuilderHolder;
import com.vladsch.flexmark.ast.FencedCodeBlock;
import com.vladsch.flexmark.ast.Node;
import com.vladsch.flexmark.ext.autolink.AutolinkExtension;
import com.vladsch.flexmark.ext.gfm.strikethrough.StrikethroughExtension;
import com.vladsch.flexmark.ext.tables.TablesExtension;
import com.vladsch.flexmark.ext.toc.TocExtension;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.html.HtmlWriter;
import com.vladsch.flexmark.html.renderer.NodeRenderer;
import com.vladsch.flexmark.html.renderer.NodeRendererContext;
import com.vladsch.flexmark.html.renderer.NodeRendererFactory;
import com.vladsch.flexmark.html.renderer.NodeRenderingHandler;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.options.DataHolder;
import com.vladsch.flexmark.util.options.MutableDataHolder;
import com.vladsch.flexmark.util.options.MutableDataSet;
import com.vladsch.flexmark.util.sequence.BasedSequence;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Set;

import static com.vladsch.flexmark.html.renderer.CoreNodeRenderer.CODE_CONTENT;

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
                        TocExtension.create()/*,
                        LineNumberCodeBlockExtension.create()*/))
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
    public String render(String title, String body) {
        StringBuilder stringBuilder = stringBuilderHolder.get();
        // TOC
        stringBuilder.append("[TOC]\n");
        // 标题
        stringBuilder.append("# ").append(title).append("\n");
        // body
        stringBuilder.append(body);
        return render(stringBuilder.toString());
    }

    @Override
    public String render(String source) {
        Node document = parser.parse(source);
        return renderer.render(document);
    }

    static class LineNumberCodeBlockExtension implements HtmlRenderer.HtmlRendererExtension {

        static LineNumberCodeBlockExtension create() {
            return new LineNumberCodeBlockExtension();
        }

        @Override
        public void rendererOptions(final MutableDataHolder options) {
            // add any configuration settings to options you want to apply to everything, here
        }

        @Override
        public void extend(final HtmlRenderer.Builder rendererBuilder, final String rendererType) {
            rendererBuilder.nodeRendererFactory(new LineNumberCodeBlockRendererFactory());
        }
    }

    static class LineNumberCodeBlockRendererFactory implements NodeRendererFactory {

        @Override
        public NodeRenderer create(DataHolder options) {
            return new LineNumberCodeBlockRenderer(options);
        }

        class LineNumberCodeBlockRenderer implements NodeRenderer {
            DataHolder options;
            private boolean codeContentBlock;

            LineNumberCodeBlockRenderer(DataHolder options) {
                this.options = options;
                codeContentBlock = Parser.FENCED_CODE_CONTENT_BLOCK.getFrom(options);
            }

            @Override
            public Set<NodeRenderingHandler<?>> getNodeRenderingHandlers() {
                return Sets.newHashSet(new NodeRenderingHandler<>(FencedCodeBlock.class, this::render));
            }

            // 复制自 com.vladsch.flexmark.html.renderer.CoreNodeRenderer.render(FencedCodeBlock node, NodeRendererContext context, HtmlWriter html)
            // 增加了行号
            void render(FencedCodeBlock node, NodeRendererContext context, HtmlWriter html) {
                html.line();
                html.srcPosWithTrailingEOL(node.getChars()).withAttr().tag("pre").openPre();

                BasedSequence info = node.getInfo();
                if (info.isNotNull() && !info.isBlank()) {
                    BasedSequence language = node.getInfoDelimitedByAny(" ");
                    html.attr("class", context.getHtmlOptions().languageClassPrefix + language.unescape());
                } else {
                    String noLanguageClass = context.getHtmlOptions().noLanguageClass.trim();
                    if (!noLanguageClass.isEmpty()) {
                        html.attr("class", noLanguageClass);
                    }
                }

                html.srcPosWithEOL(node.getContentChars()).withAttr(CODE_CONTENT).tag("code");
                if (codeContentBlock) {
                    context.renderChildren(node);
                } else {
                    String text = node.getContentChars().normalizeEOL();
                    String[] lines = text.split("\\n");
                    // 行号
                    html.tag("table");
                    int i = 0;
                    for (String line : lines) {
                        html.withAttr()
                                .tag("tr")
                                .attr("class", "line-number")
                                .attr("data-line-number", String.valueOf(++i))
                                .withAttr()
                                .tag("td").tag("/td")
                                .tag("td").text(line).tag("/td")
                                .tag("/tr");
                    }
                    html.tag("/table");
                }
                html.tag("/code");
                html.tag("/pre").closePre();
                html.lineIf(context.getHtmlOptions().htmlBlockCloseTagEol);
            }
        }
    }
}
