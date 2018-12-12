package xyz.liuw.autumn.search;

import com.vip.vjtools.vjkit.text.StringBuilderHolder;

import java.util.Set;

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
        return search(source, searchingPage -> ExactMatcher.find(searchingPage, getExpression(), getSearchStr()).getHitCount() > 0);
    }

    static class Parser extends AbstractTokenParser {
        private static StringBuilderHolder stringBuilderHolder = new StringBuilderHolder(64);

        public boolean accept(String input, int start) {
            if (input.charAt(start) != '"' || input.length() - start < 3) { // 最短 "x" 3 个字符
                return false;
            }

            StringBuilder stringBuilder = stringBuilderHolder.get();
            boolean escape = false;

            for (int i = start + 1; i < input.length(); i++) {
                char c = input.charAt(i);

                if (escape) {
                    // 只转义双引号，其余保持原样
                    if (c == '"') {
                        stringBuilder.append(c);
                    } else {
                        stringBuilder.append('\\').append(c);
                    }
                    escape = false;
                    continue;
                }

                if (c == '\\') {
                    escape = true;
                    continue;
                }

                if (c == '"') {
                    int nextIndex = i + 1;
                    if (nextIndex == input.length() || input.charAt(nextIndex) <= ' ') {
                        String expression = input.substring(start, nextIndex).toLowerCase();
                        String searchStr = stringBuilder.toString().toLowerCase();
                        token = new QuoteExactMatcher(expression, searchStr);
                        nextStart = nextIndex;
                        return true;
                    } else {
                        return false;
                    }
                }

                stringBuilder.append(c);
            }
            return false;
        }
    }
}
