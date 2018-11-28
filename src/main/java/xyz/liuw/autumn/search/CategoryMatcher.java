package xyz.liuw.autumn.search;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author liuwei
 * Created by liuwei on 2018/11/27.
 */
// category:abc
class CategoryMatcher extends AbstractMatcher {

    private CategoryMatcher(String expression, String searchStr) {
        super(expression, searchStr);
    }

    @Override
    public Set<SearchingPage> search(Set<SearchingPage> source) {
        return source.stream()
                .filter(searchingPage -> searchingPage.getPage().getCategory().equals(getSearchStr()))
                .collect(Collectors.toSet());
    }

    static class Parser extends AbstractPrefixMatcherParser {

        @Override
        protected String getPrefix() {
            return "category:";
        }

        @Override
        protected Token createMatcher(String expression, String searchStr) {
            return new CategoryMatcher(expression, searchStr);
        }
    }
}
