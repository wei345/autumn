package xyz.liuw.autumn.service;

import com.google.common.collect.Sets;
import com.google.common.io.Files;
import com.vip.vjtools.vjkit.text.StringBuilderHolder;
import com.vladsch.flexmark.ast.FencedCodeBlock;
import com.vladsch.flexmark.ast.Image;
import com.vladsch.flexmark.ast.Node;
import com.vladsch.flexmark.ext.autolink.AutolinkExtension;
import com.vladsch.flexmark.ext.gfm.strikethrough.StrikethroughExtension;
import com.vladsch.flexmark.ext.tables.TablesExtension;
import com.vladsch.flexmark.ext.toc.TocExtension;
import com.vladsch.flexmark.html.AttributeProvider;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.html.HtmlWriter;
import com.vladsch.flexmark.html.IndependentAttributeProviderFactory;
import com.vladsch.flexmark.html.renderer.*;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.html.Attributes;
import com.vladsch.flexmark.util.options.DataHolder;
import com.vladsch.flexmark.util.options.MutableDataHolder;
import com.vladsch.flexmark.util.options.MutableDataSet;
import com.vladsch.flexmark.util.sequence.BasedSequence;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.Arrays;
import java.util.Set;

import static com.vladsch.flexmark.html.renderer.CoreNodeRenderer.CODE_CONTENT;

/**
 * @author liuwei
 * Created by liuwei on 2018/11/30.
 */
@Component
public class FlexmarkMarkdownParser implements MarkdownParser {

    private static ThreadLocal<String> pathThreadLocal = new ThreadLocal<>();
    private final DataService dataService;
    private Parser parser;
    private HtmlRenderer renderer;
    private StringBuilderHolder stringBuilderHolder = new StringBuilderHolder(1024);

    @Autowired
    public FlexmarkMarkdownParser(DataService dataService) {
        this.dataService = dataService;
    }

    @PostConstruct
    private void init() {
        MutableDataSet options = new MutableDataSet()
                .set(Parser.EXTENSIONS, Arrays.asList(
                        TablesExtension.create(),
                        StrikethroughExtension.create(),
                        AutolinkExtension.create(),
                        TocExtension.create()/*,
                        LineNumberCodeBlockExtension.create()*/,
                        new SampleExtension()))
                .set(TocExtension.LEVELS, 127)
                // .set(TocExtension.LIST_CLASS, "toc"); // 顶层元素 ul.toc
                .set(TocExtension.DIV_CLASS, "toc") // 顶层元素 div.toc
                .set(TocExtension.TITLE_LEVEL, 3) // <h3>title</h3>
                .set(TocExtension.TITLE, "Table of Contents");

        parser = Parser.builder(options).build();
        renderer = HtmlRenderer.builder(options).build();
    }

    @Override
    public String render(String title, String body, String path) {
        StringBuilder markdown = stringBuilderHolder.get();
        // TOC
        markdown.append("[TOC]\n");
        // 标题
        markdown.append("# ").append(title).append("\n");
        // body
        markdown.append(body);

        pathThreadLocal.set(path);

        return render(markdown.toString());
    }

    @Override
    public String render(String source) {
        Node document = parser.parse(source);
        return renderer.render(document);
    }

    class SampleExtension implements HtmlRenderer.HtmlRendererExtension {

        @Override
        public void rendererOptions(final MutableDataHolder options) {
            // add any configuration settings to options you want to apply to everything, here
        }

        @Override
        public void extend(final HtmlRenderer.Builder rendererBuilder, final String rendererType) {
            rendererBuilder.attributeProviderFactory(new IndependentAttributeProviderFactory() {
                @Override
                public AttributeProvider create(LinkResolverContext context) {
                    return new SampleAttributeProvider(dataService);
                }
            });
        }
    }

    static class SampleAttributeProvider implements AttributeProvider {

        private DataService dataService;

        SampleAttributeProvider(DataService dataService) {
            this.dataService = dataService;
        }

        @Override
        public void setAttributes(final Node node, final AttributablePart part, final Attributes attributes) {
            if (node instanceof Image) {
                String src = attributes.getValue("src");
                // 以 http://, https://, file:/, ftp:/ ... 等等开头的都不处理
                if (src.contains(":/")) {
                    return;
                }

                int questionMark = src.indexOf('?');
                String queryString = questionMark == -1 ? "" : src.substring(questionMark);
                String path = questionMark == -1 ? src : src.substring(0, questionMark);
                String mediaPath = path.startsWith("/") ? path : Files.simplifyPath(getBasePath() + path);
                String versionKeyValue = dataService.getMediaVersionKeyValue(mediaPath);
                if (StringUtils.isBlank(versionKeyValue)) {
                    return;
                }

                if (queryString.length() == 0) {
                    queryString = "?" + versionKeyValue;
                } else {
                    char lastChar = queryString.charAt(queryString.length() - 1);
                    if (lastChar != '?' && lastChar != '&') {
                        queryString = queryString + "&";
                    }
                    queryString = queryString + versionKeyValue;
                }
                attributes.replaceValue("src", path + queryString);
            }
        }

        // 以斜线结尾 e.g. /algorithm/
        private String getBasePath() {
            String path = pathThreadLocal.get();
            int lastSlash = path.lastIndexOf('/');
            if (lastSlash == -1) {
                return "/";
            }
            return path.substring(0, lastSlash + 1);
        }
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
