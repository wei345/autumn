package xyz.liuw.autumn.search;

/**
 * @author liuwei
 * Created by liuwei on 2018/11/27.
 */
// category:abc
class CategoryMatcher extends AbstractMatcher {

    public CategoryMatcher(String expression, String expressionValue) {
        super(expression, expressionValue);
    }

    static class Parser extends AbstractPrefixTokenParser {

        @Override
        protected String getPrefix() {
            return "category:";
        }

        @Override
        protected Token createToken(String exp, String expValue) {
            return new CategoryMatcher(exp, expValue);
        }
    }
}
