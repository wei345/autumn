package xyz.liuw.autumn.search;

import java.util.Set;

/**
 * @author liuwei
 * Created by liuwei on 2018/11/27.
 */
// tag:abc
class TagMatcher extends AbstractMatcher {

    private TagMatcher(String expression, String searchStr) {
        super(expression, searchStr);
    }

    @Override
    public Set<SearchingPage> search(Set<SearchingPage> source) {
        return search(source, searchingPage -> searchingPage.getPage().getTags().contains(getSearchStr()));
    }

    static class Parser extends AbstractPrefixMatcherParser {

        @Override
        protected String getPrefix() {
            return "tag:";
        }

        @Override
        protected Token createMatcher(String expression, String searchStr) {
            return new TagMatcher(expression, searchStr);
        }
    }
}
