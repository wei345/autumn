package io.liuwei.autumn.search;

import java.util.List;

/**
 * @author liuwei
 * Created by liuwei on 2018/11/27.
 */
// -abc
class ExcludeMatcher extends AbstractPageHitMatcher {

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
    List<Hit> getHitList(String source) {
        return ExactMatcher.findHitList(source, searchStr);
    }

    @Override
    String getPageHitCacheKey() {
        return searchStr;
    }

    static class Parser extends AbstractPrefixMatcherParser {
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
