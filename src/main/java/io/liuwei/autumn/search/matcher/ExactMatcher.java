package io.liuwei.autumn.search.matcher;

import io.liuwei.autumn.search.Token;
import io.liuwei.autumn.search.model.Hit;
import io.liuwei.autumn.search.model.SearchingPage;
import io.liuwei.autumn.search.parser.AbstractPrefixMatcherParser;
import io.liuwei.autumn.util.HtmlUtil;
import io.liuwei.autumn.util.Kmp;
import org.springframework.cache.interceptor.SimpleKey;
import org.springframework.lang.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author liuwei
 * Created by liuwei on 2018/11/27.
 */
// abc
public class ExactMatcher extends AbstractPageHitMatcher {
    /**
     * 小写的 searchStr e.g. abc
     */
    private final String searchStr;

    private ExactMatcher(String expression, String searchStr) {
        super(expression);
        this.searchStr = searchStr;
    }

    @SuppressWarnings("WeakerAccess")
    public static List<Hit> findHitList(@Nullable String source, @Nullable String search) {
        if (source == null || search == null) {
            return Collections.emptyList();
        }
        List<Hit> hits = new ArrayList<>();
        int i = 0, start;
        while ((start = Kmp.indexOfIgnoreCase(source, search, i)) >= 0) {
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
    public List<Hit> getHitList(String source) {
        return findHitList(source, searchStr);
    }

    @Override
    public SimpleKey getPageHitCacheKey(SearchingPage searchingPage) {
        // expression 和 searchStr 都可以作为 cacheKey，searchStr 命中率更高，
        // 因为结果 PageHit 由 searchStr 决定，而 xxx 和 -xxx searchStr 都是 xxx
        return new SimpleKey(searchingPage.getArticle().getSnapshotId(), searchStr);
    }

    public static class Parser extends AbstractPrefixMatcherParser {

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
