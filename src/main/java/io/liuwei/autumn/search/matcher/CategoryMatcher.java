package io.liuwei.autumn.search.matcher;

import io.liuwei.autumn.search.Token;
import io.liuwei.autumn.search.model.SearchingPage;
import io.liuwei.autumn.search.parser.AbstractPrefixMatcherParser;

/**
 * @author liuwei
 * Created by liuwei on 2018/11/27.
 */
// category:abc
public class CategoryMatcher extends AbstractMatcher {

    private final String searchStr;

    private CategoryMatcher(String expression, String searchStr) {
        super(expression);
        this.searchStr = searchStr;
    }

    @Override
    protected boolean test(SearchingPage searchingPage) {
        return searchStr.equals(searchingPage.getArticle().getCategory());
    }

    public static class Parser extends AbstractPrefixMatcherParser {

        @Override
        protected String getPrefix() {
            return "c:";
        }

        @Override
        protected Token createMatcher(String expression, String searchStr) {
            return new CategoryMatcher(expression, searchStr);
        }
    }
}
