package xyz.liuw.autumn.search;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author liuwei
 * Created by liuwei on 2018/11/27.
 */
class QuoteExactMatcher extends AbstractMatcher {

    private QuoteExactMatcher(String expression, String searchStr) {
        super(expression, searchStr);
    }

    @Override
    public Set<SearchingPage> search(Set<SearchingPage> source) {
        return source.stream()
                .filter(searchingPage -> ExactMatcher.find(searchingPage, getExpression(), getSearchStr()).getHitCount() > 0)
                .collect(Collectors.toSet());
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
                        String expression = input.substring(start, i + 1).toLowerCase();
                        String searchStr = valueBuff.toString().toLowerCase();
                        token = new QuoteExactMatcher(expression, searchStr);
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
    }
}
