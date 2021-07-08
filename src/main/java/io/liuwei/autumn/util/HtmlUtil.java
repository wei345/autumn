package io.liuwei.autumn.util;

import com.google.common.io.Files;
import io.liuwei.autumn.MediaRevisionResolver;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

/**
 * @author liuwei
 * Created by liuwei on 2018/12/1.
 */
public class HtmlUtil {

    /**
     * 在给定的 html 中查找指定字符串的起始位置。相当于 HTML 版的 String#indexOf。
     * 忽略大小写，不会跨标签查找。
     * <pre><code>
     *     htmlIndexOf("ab<span>c</span>", "abc", 0)  // 返回 -1
     *     htmlIndexOf("ab<span></span>c", "abc", 0)  // 返回 -1
     *     htmlIndexOf("abc<span></span>", "abc", 0)  // 返回 0
     *     htmlIndexOf("<span>abc</span>", "abc", 0)  // 返回 6
     *     htmlIndexOf("<span>aBc</span>", "abc", 0)  // 返回 6
     * </code></pre>
     *
     * @param html   在 html 里查找 search。HTML 必须格式良好，< 和 > 必须成对出现
     * @param search 要在 html 中查找的字符串，必须已经 HTML 转义
     * @param start  html 起始（含）位置
     * @return 如果找到返回 >= 0 的整数，否则返回 -1
     */
    public static int indexOfIgnoreCase(String html, String search, int start) {
        if (html == null || search == null) {
            return -1;
        }
        if (search.length() == 0) {
            return 0;
        }
        int len = html.length();
        if (len - start < search.length()) {
            return -1;
        }
        search = search.toLowerCase();

        boolean inTag = false;
        char c;
        int j = 0;
        int maxJ = search.length() - 1;
        for (int i = start; i < len; i++) {
            c = html.charAt(i);
            if (c == '<') {
                inTag = true;
                continue;
            }
            if (c == '>') {
                j = 0;
                inTag = false;
                continue;
            }
            if (inTag) {
                continue;
            }
            if (Character.toLowerCase(c) == search.charAt(j)) {
                if (j == 0) {
                    start = i;
                }
                if (j == maxJ) {
                    return start;
                }
                j++;
            } else {
                j = 0;
            }
        }
        return -1;
    }

    public static String rewriteImgSrcAppendVersionParam(
            String html, String path, MediaRevisionResolver mediaRevisionResolver) {
        Document document = Jsoup.parse(html);
        for (Element img : document.select("img")) {
            img.attr("src",
                    appendVersionQueryParam(
                            img.attr("src"),
                            path,
                            mediaRevisionResolver));
        }
        return document.body().html();
    }

    private static String appendVersionQueryParam(
            String mediaUrl, String refererPath, MediaRevisionResolver mediaRevisionResolver) {

        // 以 http://, https://, file:/, ftp:/ ... 等等开头的都不处理
        if (mediaUrl.contains(":/")) {
            return mediaUrl;
        }

        int questionMarkPos = mediaUrl.indexOf('?');

        String mediaPath = questionMarkPos == -1 ? mediaUrl : mediaUrl.substring(0, questionMarkPos);
        if (!mediaPath.startsWith("/")) {
            // 转为绝对路径
            mediaPath = Files.simplifyPath(getDirPath(refererPath) + mediaPath);
        }
        String revision = mediaRevisionResolver.getMediaRevision(mediaPath);
        if (StringUtils.isBlank(revision)) {
            return mediaUrl;
        }

        String versionKeyValue = mediaRevisionResolver.getRevisionParamName() + "=" + revision;
        if (questionMarkPos == -1) {
            return mediaUrl + "?" + versionKeyValue;
        } else {
            return mediaUrl + "&" + versionKeyValue;
        }
    }

    /**
     * /a/b/c -> /a/b/
     */
    private static String getDirPath(String path) {
        int lastSlash = path.lastIndexOf('/');
        if (lastSlash == -1) {
            return "/";
        }
        return path.substring(0, lastSlash + 1);
    }

    public static String makeNumberedToc(String tocHtml) {
        if (tocHtml == null) {
            return null;
        }
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

    private static void makeNumberedLis(Elements lis, String prefix) {
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
