package xyz.liuw.autumn.search;

/**
 * @author liuwei
 * Created by liuwei on 2018/11/27.
 */
// abc
class ExactMatcher extends AbstractMatcher {
    public ExactMatcher(String expression, String expressionValue) {
        super(expression, expressionValue);
    }

    static class Parser extends AbstractPrefixTokenParser {

        @Override
        protected String getPrefix() {
            return "";
        }

        @Override
        protected Token createToken(String exp, String expValue) {
            return new ExactMatcher(exp, expValue);
        }
    }
}
