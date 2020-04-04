package io.liuwei.autumn.search;

import com.google.common.collect.Sets;
import com.vip.vjtools.vjkit.text.StringBuilderHolder;
import io.liuwei.autumn.data.Page;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.Elements;
import org.springframework.util.CollectionUtils;

import java.util.*;

import static com.vip.vjtools.vjkit.text.EscapeUtil.escapeHtml;
import static com.vip.vjtools.vjkit.text.EscapeUtil.urlEncode;

/**
 * @author liuwei
 * Created by liuwei on 2018/11/28.
 */
public class Highlighter {

    private static final String HL_TAG = "em";
    private static final String HL_TAG_CLASS = "search_str";
    private static final String HL_TAG_OPEN = "<" + HL_TAG + " class=\"" + HL_TAG_CLASS + "\">";
    private static final String HL_TAG_CLOSE = "</" + HL_TAG + ">";
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
    @SuppressWarnings("FieldCanBeLocal")
    private int maxPreviewLength = 120;

    private StringBuilderHolder stringBuilderHolder1 = new StringBuilderHolder(64);
    private StringBuilderHolder stringBuilderHolder2 = new StringBuilderHolder(64);


    void highlightSearchingPage(Collection<SearchingPage> searchingPages) {
        searchingPages.forEach(this::highlightSearchingPage);
    }

    private void highlightSearchingPage(SearchingPage searchingPage) {
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
                highlightHits(page.getPath(), pathHits, true));
        searchingPage.setTitlePreview(
                highlightHits(page.getTitle(), titleHits, true));
        searchingPage.setBodyPreview(
                highlightHitsLessOrEqLength(page.getBody(), new ArrayList<>(bodyHits), maxPreviewLength));

        Set<String> searchStrSet = Sets.union(getSearchStrs(titleHits), getSearchStrs(bodyHits));
        String highlightString = toHighlightString(searchStrSet);
        searchingPage.setHighlightString(highlightString);
    }

    private String highlightHits(String source, Collection<Hit> hits, boolean escape) {
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

    private String highlightHitsLessOrEqLength(String source, List<Hit> hits, int maxLength) {
        if (CollectionUtils.isEmpty(hits) || StringUtils.isBlank(source)) {
            return escapeHtml(StringUtils.substring(source, 0, maxLength));
        }

        // 尽可能多包含 searchStr 也不一定好，因为可能会命中一段代码，而代码之前的段落文字可能更有意义。
        //hits = mostAdjacent(hits, maxLength);

        StringBuilder sb1 = stringBuilderHolder1.get();
        int start = hits.get(0).getStart();
        int begin = start;
        int textLength = 0;
        for (Hit hit : hits) {
            int len = textLength + (hit.getStart() - start) + (hit.getEnd() - hit.getStart());

            if (textLength > 0 && len > maxLength) {
                break;
            }
            sb1.append(htmlEscape(source, start, hit.getStart()))
                    .append(HL_TAG_OPEN)
                    .append(htmlEscape(source, hit.getStart(), hit.getEnd()))
                    .append(HL_TAG_CLOSE);
            textLength += (hit.getStart() - start) + (hit.getEnd() - hit.getStart());
            start = hit.getEnd();
        }

        // 均匀往两边扩充，达到 maxLength
        int left = begin;
        int right = start;
        while (textLength < maxLength && (left > 0 || right < source.length())) {
            if (left > 0) {
                left--;
                textLength++;
            }
            if (right < source.length()) {
                right++;
                textLength++;
            }
        }
        StringBuilder sb2 = stringBuilderHolder2.get();
        if (left < begin) {
            sb2.append(htmlEscape(source, left, begin)).append(sb1);
        }
        if (start < right) {
            if (sb2.length() == 0) {
                sb2.append(sb1);
            }
            sb2.append(htmlEscape(source, start, right));
        }
        if (sb2.length() == 0) {
            sb2 = sb1;
        }

        return sb2.toString();
    }

    /**
     * 返回跨度不超过 maxLength，尽可能多的相邻的 Hits。
     */
    private List<Hit> mostAdjacent(List<Hit> hits, int maxLength) {
        Validate.notEmpty(hits);
        Validate.isTrue(maxLength > 0);

        List<Hit> result = new ArrayList<>(4);
        List<Hit> tmp = new ArrayList<>(4);
        for (int i = 0; i < hits.size(); i++) {
            tmp.clear();
            for (int j = i; j < hits.size(); j++) {
                Hit hit = hits.get(j);
                if (tmp.size() == 0 || (hit.getEnd() - tmp.get(0).getStart()) <= maxLength) {
                    tmp.add(hit);
                } else {
                    break;
                }
            }
            if (tmp.size() > result.size()) {
                List<Hit> t = result;
                result = tmp;
                tmp = t;
            }
        }
        return result;
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

    private String htmlEscape(String str, int start, int end) {
        return escapeHtml(str.substring(start, end));
    }

    /* 这个实现有 bug，例如原文中有 ">"，html 转义为 "&gt;"，高亮 "gt;"，期望 "&gt;"，实际 "&<em>gt;</em>"
    public String highlightSearchStr(String html, List<String> searchStrList) {
        Set<Hit> hits = Sets.newTreeSet(HIT_COMPARATOR);
        searchStrList.forEach(s -> hits.addAll(ExactMatcher.htmlFindHitList(html, escapeHtml(s))));
        return highlightHits(html, hits, false);
    }*/

    // highlight using jsoup
    public String highlightSearchStr(String html, List<String> searchStrList) {
        Document document = Jsoup.parse(html);
        Elements allElements = document.getAllElements();
        for (Element element : allElements) {
            for (TextNode textNode : element.textNodes()) {
                highlight(textNode, searchStrList);
            }
        }
        return document.html();
    }

    private void highlight(TextNode textNode, List<String> searchStrList) {
        String text = textNode.getWholeText();
        if (StringUtils.isBlank(text)) {
            return;
        }

        Set<Hit> hits = Sets.newTreeSet(HIT_COMPARATOR);
        searchStrList.forEach(s -> hits.addAll(ExactMatcher.findHitList(text, s)));

        int start = 0;
        for (Hit hit : hits) {
            textNode.before(new TextNode(text.substring(start, hit.getStart())));
            Element em = new Element(HL_TAG);
            em.addClass(HL_TAG_CLASS);
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
