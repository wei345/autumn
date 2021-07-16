package io.liuwei.autumn.search.matcher;

import io.liuwei.autumn.search.Token;
import io.liuwei.autumn.search.model.SearchingPage;
import io.liuwei.autumn.search.parser.AbstractPrefixMatcherParser;

import java.util.Set;

/**
 * @author liuwei
 * Created by liuwei on 2018/11/27.
 */
// tag:abc
public class TagMatcher extends AbstractMatcher {

    private final String searchStr;

    private TagMatcher(String expression, String searchStr) {
        super(expression);
        this.searchStr = searchStr;
    }

    @Override
    protected boolean test(SearchingPage searchingPage) {
        Set<String> tags = searchingPage.getArticle().getTags();
        return tags != null && tags.contains(searchStr);
    }

    public static class Parser extends AbstractPrefixMatcherParser {

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
