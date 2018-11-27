package xyz.liuw.autumn.search;

/**
 * @author liuwei
 * Created by liuwei on 2018/11/27.
 */
// tag:abc
class TagMatcher extends AbstractMatcher {

    public TagMatcher(String expression, String expressionValue) {
        super(expression, expressionValue);
    }

    static class Parser extends AbstractPrefixTokenParser {

        @Override
        protected String getPrefix() {
            return "tag:";
        }

        @Override
        protected Token createToken(String exp, String expValue) {
            return new TagMatcher(exp, expValue);
        }
    }
}
