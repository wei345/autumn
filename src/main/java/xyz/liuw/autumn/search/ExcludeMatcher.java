package xyz.liuw.autumn.search;

/**
 * @author liuwei
 * Created by liuwei on 2018/11/27.
 */
// -abc
class ExcludeMatcher extends AbstractMatcher {

    public ExcludeMatcher(String expression, String expressionValue) {
        super(expression, expressionValue);
    }

    static class Parser extends AbstractPrefixTokenParser {
        @Override
        protected String getPrefix() {
            return "-";
        }

        @Override
        protected Token createToken(String exp, String expValue) {
            return new ExcludeMatcher(exp, expValue);
        }

        /*@Override
        public TokenParserAcceptReturn accept(char c, int i) {
            if (i == start) {
                Validate.isTrue(c == '-');
                if (i == input.length() - 1) {
                    return ABORT;
                }
                return CONTINUE;
            }

            if (c <= ' ') {

                if (valueBuff.length() == 0) {
                    return ABORT;
                }

                createMatcher(i);
                return FINISH;
            }

            valueBuff.append(c);

            if (i == input.length() - 1) {
                createMatcher(i);
                return FINISH_CLOSE;
            }

            return CONTINUE;
        }

        private void createMatcher(int i) {
            String exp = input.substring(start, i + 1);
            matcher = new ExcludeMatcher(exp, valueBuff.toString());
        }*/
    }

}
