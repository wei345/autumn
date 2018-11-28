package xyz.liuw.autumn.search;

import java.util.Set;

/**
 * @author liuwei
 * Created by liuwei on 2018/11/27.
 */
// -abc
class ExcludeMatcher extends AbstractMatcher {

    private ExcludeMatcher(String expression, String searchStr) {
        super(expression, searchStr);
    }

    @Override
    public Set<SearchingPage> search(Set<SearchingPage> source) {
        return search(source, searchingPage -> ExactMatcher.find(searchingPage, getExpression(), getSearchStr()).getHitCount() == 0);
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
