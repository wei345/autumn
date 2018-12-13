package xyz.liuw.autumn.search;

import org.apache.commons.lang3.StringUtils;
import org.springframework.lang.Nullable;
import xyz.liuw.autumn.util.HtmlUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author liuwei
 * Created by liuwei on 2018/11/27.
 */
// abc
class ExactMatcher extends AbstractPageHitMatcher {
    /**
     * 小写的 searchStr e.g. abc
     */
    private String searchStr;

    private ExactMatcher(String expression, String searchStr) {
        super(expression);
        this.searchStr = searchStr;
    }

    @SuppressWarnings("WeakerAccess")
    static List<Hit> findHitList(@Nullable String source, @Nullable String search) {
        if (source == null || search == null) {
            return Collections.emptyList();
        }
        List<Hit> hits = new ArrayList<>();
        int i = 0, start;
        while ((start = StringUtils.indexOfIgnoreCase(source, search, i)) >= 0) {
            i = start + search.length();
            hits.add(new Hit(start, i, search));
        }
        return hits;
    }

    static List<Hit> htmlFindHitList(@Nullable String source, @Nullable String search) {
        if (source == null || search == null) {
            return Collections.emptyList();
        }
        List<Hit> hits = new ArrayList<>();
        int i = 0, start;
        while ((start = HtmlUtil.indexOfIgnoreCase(source, search, i)) >= 0) {
            i = start + search.length();
            hits.add(new Hit(start, i, search));
        }
        return hits;
    }

    @Override
    protected boolean test(SearchingPage searchingPage) {
        return getPageHit(searchingPage).getHitCount() > 0;
    }

    @Override
    List<Hit> getHitList(String source) {
        return findHitList(source, searchStr);
    }

    @Override
    String getPageHitCacheKey() {
        // expression 和 searchStr 都可以作为 cacheKey，searchStr 命中率更高，
        // 因为结果 PageHit 由 searchStr 决定，而 xxx 和 -xxx searchStr 都是 xxx
        return searchStr;
    }

    static class Parser extends AbstractPrefixMatcherParser {

        @Override
        protected String getPrefix() {
            return "";
        }

        @Override
        protected Token createMatcher(String expression, String searchStr) {
            return new ExactMatcher(expression, searchStr);
        }
    }
}
