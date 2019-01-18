package xyz.liuw.autumn.service;

import com.google.common.collect.Sets;
import com.google.common.io.Files;
import com.vip.vjtools.vjkit.text.StringBuilderHolder;
import com.vladsch.flexmark.ast.Image;
import com.vladsch.flexmark.ast.Node;
import com.vladsch.flexmark.ext.autolink.AutolinkExtension;
import com.vladsch.flexmark.ext.gfm.strikethrough.StrikethroughExtension;
import com.vladsch.flexmark.ext.tables.TableBlock;
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
import com.vladsch.flexmark.util.options.DataKey;
import com.vladsch.flexmark.util.options.MutableDataHolder;
import com.vladsch.flexmark.util.options.MutableDataSet;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import xyz.liuw.autumn.data.Page;

import javax.annotation.PostConstruct;
import java.util.Arrays;
import java.util.Set;
import java.util.UUID;

/**
 * @author liuwei
 * Created by liuwei on 2018/11/30.
 */
@Component
public class FlexmarkMarkdownParser implements MarkdownParser {

    private static final String BOUNDARY = "<hr id='" + UUID.randomUUID().toString() + "'>";
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
                        ScrollTableExtension.create(),
                        StrikethroughExtension.create(),
                        AutolinkExtension.create(),
                        TocExtension.create(),
                        new MediaVersionExtension(dataService)))
                .set(HtmlRenderer.FENCED_CODE_LANGUAGE_CLASS_PREFIX, "")
                .set(ScrollTableExtension.CLASS_NAME, "scroll_table")
                .set(TocExtension.LEVELS, 0b01111111) // H1 .. H7
                .set(TocExtension.DIV_CLASS, "toc") // <div class="toc">
                .set(TocExtension.TITLE_LEVEL, 3) // <h3>title</h3>
                .set(TocExtension.TITLE, "TOC");

        parser = Parser.builder(options).build();
        renderer = HtmlRenderer.builder(options).build();
    }

    @Override
    public Page.PageHtml render(String title, String body, String path) {

        String markdown = stringBuilderHolder.get()
                .append("[TOC]\n")
                .append(BOUNDARY).append("\n\n") // BOUNDARY 之后空一行，否则后面的 markdown 不解析
                .append("# ").append(title).append("\n\n") // title 也可以出现在 TOC 中
                .append(BOUNDARY).append("\n\n")
                .append(body)
                .toString();

        pathThreadLocal.set(path);
        String html = render(markdown);

        int boundary1Start = html.indexOf(BOUNDARY);
        int boundary2Start = html.indexOf(BOUNDARY, boundary1Start + BOUNDARY.length());
        String toc = html.substring(0, boundary1Start);
        String titleH1 = html.substring(boundary1Start + BOUNDARY.length(), boundary2Start);
        String content = html.substring(boundary2Start + BOUNDARY.length());
        return new Page.PageHtml(toc, titleH1, content);
    }

    private String render(String source) {
        Node document = parser.parse(source);
        return renderer.render(document);
    }

    static class MediaVersionExtension implements HtmlRenderer.HtmlRendererExtension {

        private final DataService dataService;

        MediaVersionExtension(DataService dataService) {
            this.dataService = dataService;
        }

        @Override
        public void rendererOptions(final MutableDataHolder options) {
        }

        @Override
        public void extend(final HtmlRenderer.Builder rendererBuilder, final String rendererType) {
            rendererBuilder.attributeProviderFactory(new IndependentAttributeProviderFactory() {
                @Override
                public AttributeProvider create(LinkResolverContext context) {
                    return new MediaAttributeProvider(dataService);
                }
            });
        }

        static class MediaAttributeProvider implements AttributeProvider {

            private DataService dataService;

            MediaAttributeProvider(DataService dataService) {
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
    }

    static class ScrollTableExtension implements HtmlRenderer.HtmlRendererExtension {
        static final DataKey<String> CLASS_NAME = new DataKey<>("CLASS_NAME", "scroll-table");

        static ScrollTableExtension create() {
            return new ScrollTableExtension();
        }

        @Override
        public void rendererOptions(final MutableDataHolder options) {
            // add any configuration settings to options you want to apply to everything, here
        }

        @Override
        public void extend(final HtmlRenderer.Builder rendererBuilder, final String rendererType) {
            rendererBuilder.nodeRendererFactory(new ScrollTableRendererFactory());
        }

        static class ScrollTableRendererFactory implements NodeRendererFactory {
            @Override
            public NodeRenderer create(DataHolder options) {
                return new ScrollTableRenderer(options);
            }
        }

        static class ScrollTableRenderer implements NodeRenderer {
            private String className;

            ScrollTableRenderer(DataHolder options) {
                this.className = CLASS_NAME.getFrom(options);
            }

            @Override
            public Set<NodeRenderingHandler<?>> getNodeRenderingHandlers() {
                return Sets.newHashSet(new NodeRenderingHandler<>(TableBlock.class, this::render));
            }

            private void render(final TableBlock node, final NodeRendererContext context, HtmlWriter html) {
                html.attr("class", className).withAttr().tag("div");
                context.delegateRender();
                html.tag("/div");
            }
        }
    }
}
