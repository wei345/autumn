package xyz.liuw.autumn.search;

/**
 * @author liuwei
 * Created by liuwei on 2018/11/27.
 */
class QuoteExactMatcher extends AbstractMatcher {

    private QuoteExactMatcher(String expression, String expressionValue) {
        super(expression, expressionValue);
    }

    static class Parser extends AbstractTokenParser {

        public boolean accept(String input, int start) {
            if (input.charAt(start) != '"' || input.length() - start < 3) {
                return false;
            }

            StringBuilder valueBuff = new StringBuilder(input.length() - start);
            boolean escape = false;

            for (int i = start + 1; i < input.length(); i++) {
                char c = input.charAt(i);

                if (escape) {
                    // 只转义双引号，其余保存原样
                    if (c == '"') {
                        valueBuff.append(c);
                    } else {
                        valueBuff.append('\\').append(c);
                    }
                    escape = false;
                    continue;
                }

                if (c == '\\') {
                    escape = true;
                    continue;
                }

                if (c == '"') {
                    if (i == input.length() - 1 || input.charAt(i + 1) <= ' ') {
                        String expression = input.substring(start, i + 1);
                        token = new QuoteExactMatcher(expression, valueBuff.toString());
                        nextStart = i + 1;
                        return true;
                    } else {
                        return false;
                    }
                }

                valueBuff.append(c);
            }
            return false;
        }

        /*Parser(String input, int start) {
            super(input, start);
        }

        @Override
        public TokenParserAcceptReturn accept(char c, int i) {
            if (i == start) {
                Validate.isTrue(c == '"');
                if (i == input.length() - 1) {
                    return ABORT; // i = start - 1
                }
                return CONTINUE;
            }

            if (escape) {
                // 只转义双引号，其余保存原样
                if (c == '"') {
                    valueBuff.append(c);
                } else {
                    valueBuff.append('\\').append(c);
                }
                escape = false;
                return CONTINUE;
            }

            if (c == '\\') {
                escape = true;
                return CONTINUE;
            }

            if (c == '"') {
                String expression = input.substring(start, i + 1);
                matcher = new QuoteExactMatcher(expression, valueBuff.toString());
                return FINISH_CLOSE; // start = i + 1;
            }

            if (i == input.length() - 1) {
                return ABORT; // i = start - 1
            }

            valueBuff.append(c);
            return CONTINUE;
        }*/
    }
}
