package xyz.liuw.autumn.search;

/**
 * @author liuwei
 * Created by liuwei on 2018/11/27.
 */
// category:abc
class CategoryMatcher extends AbstractMatcher {

    private String searchStr;

    private CategoryMatcher(String expression, String searchStr) {
        super(expression);
        this.searchStr = searchStr;
    }

    @Override
    protected boolean test(SearchingPage searchingPage) {
        return searchStr.equals(searchingPage.getPage().getCategory());
    }

    static class Parser extends AbstractPrefixMatcherParser {

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
