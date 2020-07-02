package io.liuwei.autumn.converter;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Sets;
import com.vip.vjtools.vjkit.text.StringBuilderHolder;
import com.vladsch.flexmark.ast.Node;
import com.vladsch.flexmark.ext.autolink.AutolinkExtension;
import com.vladsch.flexmark.ext.gfm.strikethrough.StrikethroughExtension;
import com.vladsch.flexmark.ext.tables.TableBlock;
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
import com.vladsch.flexmark.util.options.DataKey;
import com.vladsch.flexmark.util.options.MutableDataHolder;
import com.vladsch.flexmark.util.options.MutableDataSet;
import io.liuwei.autumn.domain.Page;
import io.liuwei.autumn.service.DataService;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.Arrays;
import java.util.Set;
import java.util.UUID;

/**
 * @author liuwei
 * Created by liuwei on 2018/11/30.
 */
@Component
public class MarkdownPageConverter extends AbstractPageConverter {

    private static final String BOUNDARY = "<hr id='" + UUID.randomUUID().toString() + "'>";
    private final StringBuilderHolder stringBuilderHolder = new StringBuilderHolder(1024);
    private Parser parser;
    private HtmlRenderer renderer;

    @Autowired
    public MarkdownPageConverter(DataService dataService) {
        super(dataService);
    }

    @PostConstruct
    private void init() {
        MutableDataSet options = new MutableDataSet()
                .set(Parser.EXTENSIONS, Arrays.asList(
                        TablesExtension.create(),
                        ScrollTableExtension.create(),
                        StrikethroughExtension.create(),
                        AutolinkExtension.create(),
                        TocExtension.create()))
                .set(HtmlRenderer.FENCED_CODE_LANGUAGE_CLASS_PREFIX, "")
                .set(ScrollTableExtension.CLASS_NAME, "scroll_table")
                .set(TocExtension.LEVELS, 0b11111110) // H7 .. H1
                .set(TocExtension.DIV_CLASS, "toc") // <div class="toc">
                .set(TocExtension.TITLE_LEVEL, 3) // <h3>title</h3>
                .set(TocExtension.TITLE, "TOC");

        parser = Parser.builder(options).build();
        renderer = HtmlRenderer.builder(options).build();
    }

    @Override
    protected Page.PageHtml parse(String title, String body) {
        String markdown = stringBuilderHolder.get()
                .append("[TOC]\n")
                .append(BOUNDARY).append("\n\n") // BOUNDARY 之后空一行，否则后面的 markdown 不解析
                .append("# ").append(title).append("\n\n") // title 也可以出现在 TOC 中
                .append(BOUNDARY).append("\n\n")
                .append(body)
                .toString();

        String html = render(markdown);

        int boundary1Start = html.indexOf(BOUNDARY);
        int boundary2Start = html.indexOf(BOUNDARY, boundary1Start + BOUNDARY.length());
        String toc = makeNumberedToc(html.substring(0, boundary1Start));
        String titleHtml = html.substring(boundary1Start + BOUNDARY.length(), boundary2Start);
        String content = html.substring(boundary2Start + BOUNDARY.length());
        return new Page.PageHtml(toc, titleHtml, content);
    }

    @VisibleForTesting
    String makeNumberedToc(String tocHtml) {
        Document document = Jsoup.parse(tocHtml);
        Elements topLis = document.select("div > ul > li");
        if (topLis == null || topLis.size() == 0 || document.select("li").size() < 3) {
            return "";
        }

        Elements startLis;
        if (topLis.size() == 1) {
            Elements uls = topLis.select("ul");
            if (uls == null || uls.size() == 0) {
                return tocHtml;
            }
            startLis = uls.first().children();
        }
        // topLis.size() > 1
        else {
            startLis = topLis;
        }

        if (startLis == null || startLis.size() == 0) {
            return tocHtml;
        }

        makeNumberedLis(startLis, "");
        return document.select("div").outerHtml();
    }

    private void makeNumberedLis(Elements lis, String prefix) {
        if (lis == null || lis.size() == 0) {
            return;
        }
        int i = 1;
        for (Element li : lis) {
            // 替换
            // <a href="#xxx">xxx</a>
            // 为：
            // <a href="#xxx"><span class="tocnumber">1</span><span class="toctext">xxx</span></a>
            String num = prefix + (i++);
            Element a = li.selectFirst("a");
            Element textSpan = new Element("span").addClass("toctext").text(a.text());
            a.textNodes().forEach(org.jsoup.nodes.Node::remove);
            a.insertChildren(0, textSpan)
                    .insertChildren(0, new Element("span").addClass("tocnumber").text(num));
            // 递归子节点
            Elements uls = li.select("ul");
            if (uls != null && uls.size() > 0) {
                makeNumberedLis(uls.first().children(), num + ".");
            }
        }
    }

    private String render(String source) {
        Node document = parser.parse(source);
        return renderer.render(document);
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
