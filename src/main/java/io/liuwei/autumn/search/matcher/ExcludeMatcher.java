package io.liuwei.autumn.search.matcher;

import io.liuwei.autumn.search.parser.AbstractPrefixMatcherParser;
import io.liuwei.autumn.search.model.Hit;
import io.liuwei.autumn.search.model.SearchingPage;
import io.liuwei.autumn.search.Token;
import org.springframework.cache.interceptor.SimpleKey;

import java.util.List;

/**
 * @author liuwei
 * Created by liuwei on 2018/11/27.
 */
// -abc
public class ExcludeMatcher extends AbstractPageHitMatcher {

    private String searchStr;

    private ExcludeMatcher(String expression, String searchStr) {
        super(expression);
        this.searchStr = searchStr;
    }

    @Override
    protected boolean test(SearchingPage searchingPage) {
        return getPageHit(searchingPage).getHitCount() == 0;
    }

    @Override
    public List<Hit> getHitList(String source) {
        return ExactMatcher.findHitList(source, searchStr);
    }

    @Override
    public SimpleKey getPageHitCacheKey(SearchingPage searchingPage) {
        return new SimpleKey(searchingPage.getArticle().getSnapshotId(), searchStr);
    }

    public static class Parser extends AbstractPrefixMatcherParser {
        @Override
        protected String getPrefix() {
            return "-";
        }

        @Override
        protected Token createMatcher(String expression, String searchStr) {
            return new ExcludeMatcher(expression, searchStr);
        }
    }

}
