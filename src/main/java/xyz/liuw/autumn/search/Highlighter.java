package xyz.liuw.autumn.search;

import com.google.common.collect.Sets;
import com.vip.vjtools.vjkit.text.StringBuilderHolder;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.Elements;
import org.springframework.util.CollectionUtils;
import xyz.liuw.autumn.data.Page;

import java.util.*;

import static com.vip.vjtools.vjkit.text.EscapeUtil.escapeHtml;
import static com.vip.vjtools.vjkit.text.EscapeUtil.urlEncode;

/**
 * @author liuwei
 * Created by liuwei on 2018/11/28.
 */
public class Highlighter {

    private static final String HL_TAG_OPEN = "<em>";
    private static final String HL_TAG_CLOSE = "</em>";
    // 获得不重叠的 hit list
    private static Comparator<Hit> HIT_COMPARATOR = (o1, o2) -> {
        // o1 在 o2 前面，无重叠
        if (o1.getStart() < o2.getStart() && o2.getStart() >= o1.getEnd()) {
            return -1;
        }
        // o2 在 o1 前面，无重叠
        if (o2.getStart() < o1.getStart() && o1.getStart() >= o2.getEnd()) {
            return 1;
        }
        // o1 和 o2 只留一个
        return 0;
    };
    private int maxPreviewLength = 120;



    void highlightHits(Collection<SearchingPage> searchingPages) {
        searchingPages.forEach(this::highlightHits);
    }

    private void highlightHits(SearchingPage searchingPage) {
        Page page = searchingPage.getPage();
        if (CollectionUtils.isEmpty(searchingPage.getUnmodifiableHitMap())) {
            searchingPage.setPathPreview(escapeHtml(page.getPath()));
            searchingPage.setTitlePreview(escapeHtml(page.getTitle()));
            searchingPage.setBodyPreview(escapeHtml(
                    StringUtils.substring(page.getBody(), 0, maxPreviewLength)));
            return;
        }

        TreeSet<Hit> pathHits = Sets.newTreeSet(HIT_COMPARATOR);
        TreeSet<Hit> titleHits = Sets.newTreeSet(HIT_COMPARATOR);
        TreeSet<Hit> bodyHits = Sets.newTreeSet(HIT_COMPARATOR);
        for (Map.Entry<String, PageHit> entry : searchingPage.getUnmodifiableHitMap().entrySet()) {
            PageHit pageHit = entry.getValue();
            pathHits.addAll(pageHit.getPathHitList());
            titleHits.addAll(pageHit.getTitleHitList());
            bodyHits.addAll(pageHit.getBodyHitList());
        }

        searchingPage.setPathPreview(
                highlight(page.getPath(), pathHits, true));
        searchingPage.setTitlePreview(
                highlight(page.getTitle(), titleHits, true));
        searchingPage.setBodyPreview(
                highlight(page.getBody(), new ArrayList<>(bodyHits), maxPreviewLength));

        Set<String> searchStrSet = Sets.union(getSearchStrs(titleHits), getSearchStrs(bodyHits));
        String highlightString = toHighlightString(searchStrSet);
        searchingPage.setHighlightString(highlightString);
    }

    private String highlight(String source, Collection<Hit> hits, boolean escape) {
        if (CollectionUtils.isEmpty(hits) || StringUtils.isBlank(source)) {
            return escape ? escapeHtml(source) : source;
        }

        StringBuilder stringBuilder = StringBuilderHolder.getGlobal();
        int start = 0;
        for (Hit hit : hits) {
            if (escape) {
                stringBuilder.append(htmlEscape(source, start, hit.getStart()));
            } else {
                stringBuilder.append(source, start, hit.getStart());
            }

            stringBuilder.append(HL_TAG_OPEN);

            if (escape) {
                stringBuilder.append(htmlEscape(source, hit.getStart(), hit.getEnd()));
            } else {
                stringBuilder.append(source, hit.getStart(), hit.getEnd());
            }

            stringBuilder.append(HL_TAG_CLOSE);
            start = hit.getEnd();
        }
        if (start == 0) {
            return source;
        }
        if (start < source.length()) {
            if (escape) {
                stringBuilder.append(htmlEscape(source, start, source.length()));
            } else {
                stringBuilder.append(source, start, source.length());
            }
        }
        return stringBuilder.toString();
    }

    private String highlight(String source, List<Hit> hits, int maxLength) {
        if (CollectionUtils.isEmpty(hits) || StringUtils.isBlank(source)) {
            return escapeHtml(StringUtils.substring(source, 0, maxLength));
        }

        StringBuilder stringBuilder = StringBuilderHolder.getGlobal();

        int start = hits.get(0).getStart() - 3;
        if (start < 0) {
            start = 0;
        }
        for (Hit hit : hits) {
            int len = stringBuilder.length() +
                    (hit.getStart() - start) +
                    HL_TAG_OPEN.length() +
                    (hit.getEnd() - hit.getStart()) +
                    HL_TAG_CLOSE.length();

            if (stringBuilder.length() > 0 && len > maxLength) {
                break;
            }
            stringBuilder.append(htmlEscape(source, start, hit.getStart()))
                    .append(HL_TAG_OPEN)
                    .append(htmlEscape(source, hit.getStart(), hit.getEnd()))
                    .append(HL_TAG_CLOSE);
            start = hit.getEnd();
        }

        if (stringBuilder.length() < maxLength) {
            int end = Math.min(start + (maxLength - stringBuilder.length()), source.length());
            if (end > start) {
                stringBuilder.append(htmlEscape(source, start, end));
            }
        }
        return stringBuilder.toString();
    }

    private Set<String> getSearchStrs(Set<Hit> hits) {
        if (CollectionUtils.isEmpty(hits)) {
            return Collections.emptySet();
        }
        Set<String> set = new HashSet<>(4);
        for (Hit hit : hits) {
            if (StringUtils.isNotBlank(hit.getStr())) {
                set.add(hit.getStr());
            }
        }
        return set;
    }

    private String toHighlightString(Collection<String> searchStrs) {
        StringBuilder sb = StringBuilderHolder.getGlobal();
        searchStrs.forEach(s -> sb.append("h=").append(urlEncode(s)).append("&"));
        if (sb.length() > 0) {
            sb.setLength(sb.length() - 1);
        }
        return sb.toString();
    }

    // apply highlight string
    public String highlightSearchStr(String html, List<String> searchStrList) {
        Set<Hit> hits = Sets.newTreeSet(HIT_COMPARATOR);
        searchStrList.forEach(s -> hits.addAll(ExactMatcher.htmlFind(html, escapeHtml(s))));
        return highlight(html, hits, false);
    }

    private String htmlEscape(String str, int start, int end) {
        return escapeHtml(str.substring(start, end));
    }

    /*public String highlightSearchStr(String html, List<String> searchStrList) {
        Document document = Jsoup.parse(html);
        Elements allElements = document.getAllElements();
        for (Element element : allElements) {
            for (TextNode textNode : element.textNodes()) {
                highlight(textNode, searchStrList);
            }
        }
        return document.html();
    }*/

    private void highlight(TextNode textNode, List<String> searchStrList) {
        String text = textNode.text();
        if (StringUtils.isBlank(text)) {
            return;
        }

        Set<Hit> hits = Sets.newTreeSet(HIT_COMPARATOR);
        searchStrList.forEach(s -> hits.addAll(ExactMatcher.find(text, s)));

        int start = 0;
        for (Hit hit : hits) {
            textNode.before(new TextNode(text.substring(start, hit.getStart())));
            Element em = new Element("em");
            em.appendChild(new TextNode(text.substring(hit.getStart(), hit.getEnd())));
            textNode.before(em);
            start = hit.getEnd();
        }

        if (start == 0) {
            return;
        }

        if (start < text.length()) {
            textNode.before(new TextNode(text.substring(start)));
        }

        textNode.remove();
    }
}
