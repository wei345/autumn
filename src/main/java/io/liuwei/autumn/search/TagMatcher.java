package io.liuwei.autumn.search;

import java.util.Set;

/**
 * @author liuwei
 * Created by liuwei on 2018/11/27.
 */
// tag:abc
class TagMatcher extends AbstractMatcher {

    private String searchStr;

    private TagMatcher(String expression, String searchStr) {
        super(expression);
        this.searchStr = searchStr;
    }

    @Override
    protected boolean test(SearchingPage searchingPage) {
        Set<String> tags = searchingPage.getArticle().getTags();
        return tags != null && tags.contains(searchStr);
    }

    static class Parser extends AbstractPrefixMatcherParser {

        @Override
        protected String getPrefix() {
            return "t:";
        }

        @Override
        protected Token createMatcher(String expression, String searchStr) {
            return new TagMatcher(expression, searchStr);
        }
    }
}
