package io.liuwei.autumn.service;

import com.google.common.io.Files;
import com.vip.vjtools.vjkit.text.EscapeUtil;
import io.liuwei.autumn.data.Page;
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
        return new Page.PageHtml(pageHtml.getToc(), pageHtml.getTitle(), bodyHtml);
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


}
