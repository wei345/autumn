package io.liuwei.autumn.converter;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.io.Files;
import io.liuwei.autumn.domain.Page;
import io.liuwei.autumn.service.DataService;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

/**
 * @author liuwei
 * @since 2020-07-01 18:03
 */
public abstract class AbstractPageConverter implements PageConverter {

    private final DataService dataService;

    public AbstractPageConverter(DataService dataService) {
        this.dataService = dataService;
    }

    @Override
    public Page.PageHtml convert(String title, String body, String path) {
        Page.PageHtml pageHtml = parse(title, body);
        String bodyHtml = rewriteImageSrc(pageHtml.getContent(), path);
        String tocHtml = makeNumberedToc(pageHtml.getToc());
        return new Page.PageHtml(tocHtml, pageHtml.getTitle(), bodyHtml);
    }

    protected abstract Page.PageHtml parse(String title, String body);

    private String rewriteImageSrc(String html, String path) {
        Document document = Jsoup.parse(html);

        Elements imgs = document.select("img");
        for (Element img : imgs) {
            img.attr("src", appendVersion(img.attr("src"), path));
        }

        return document.body().html();
    }


    private String appendVersion(String src, String pagePath) {
        // 以 http://, https://, file:/, ftp:/ ... 等等开头的都不处理
        if (src.contains(":/")) {
            return src;
        }

        int questionMark = src.indexOf('?');
        String queryString = questionMark == -1 ? "" : src.substring(questionMark);
        String path = questionMark == -1 ? src : src.substring(0, questionMark);
        @SuppressWarnings("UnstableApiUsage")
        String fullPath = path.startsWith("/") ? path : Files.simplifyPath(getBasePath(pagePath) + path);
        String versionKeyValue = dataService.getMediaVersionKeyValue(fullPath);
        if (StringUtils.isBlank(versionKeyValue)) {
            return src;
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
        return path + queryString;
    }

    // 以斜线结尾 e.g. /algorithm/
    private String getBasePath(String path) {
        int lastSlash = path.lastIndexOf('/');
        if (lastSlash == -1) {
            return "/";
        }
        return path.substring(0, lastSlash + 1);
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
        } else { // topLis.size() > 1
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
}
