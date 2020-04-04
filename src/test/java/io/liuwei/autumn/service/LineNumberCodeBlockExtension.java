package io.liuwei.autumn.service;

import com.google.common.collect.Sets;
import com.vladsch.flexmark.ast.FencedCodeBlock;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.html.HtmlWriter;
import com.vladsch.flexmark.html.renderer.NodeRenderer;
import com.vladsch.flexmark.html.renderer.NodeRendererContext;
import com.vladsch.flexmark.html.renderer.NodeRendererFactory;
import com.vladsch.flexmark.html.renderer.NodeRenderingHandler;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.options.DataHolder;
import com.vladsch.flexmark.util.options.MutableDataHolder;
import com.vladsch.flexmark.util.sequence.BasedSequence;

import java.util.Set;

import static com.vladsch.flexmark.html.renderer.CoreNodeRenderer.CODE_CONTENT;

/**
 * @author liuwei
 * Created by liuwei on 2019/1/2.
 */
public class LineNumberCodeBlockExtension implements HtmlRenderer.HtmlRendererExtension {

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

    static class LineNumberCodeBlockRendererFactory implements NodeRendererFactory {
        @Override
        public NodeRenderer create(DataHolder options) {
            return new LineNumberCodeBlockRenderer(options);
        }
    }

    static class LineNumberCodeBlockRenderer implements NodeRenderer {
        private DataHolder options;
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
        private void render(FencedCodeBlock node, NodeRendererContext context, HtmlWriter html) {
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

/*
pre code table {
    margin: 0;
    padding-right: 15px;
    width: auto;
}

pre code table td {
    border: 0;
    padding: 0;
}

pre code table td.line-number {
    min-width: 1em;
    text-align: right;
    padding-right: 0.8em;
    color: rgba(27, 31, 35, .3);
}

pre code table td.line-number:before {
    content: attr(data-line-number);
}
*/



