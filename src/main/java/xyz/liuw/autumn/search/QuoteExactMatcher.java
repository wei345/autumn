package xyz.liuw.autumn.search;

import com.vip.vjtools.vjkit.text.StringBuilderHolder;

import java.util.Set;

/**
 * @author liuwei
 * Created by liuwei on 2018/11/27.
 */
class QuoteExactMatcher extends AbstractMatcher {

    private static StringBuilderHolder stringBuilderHolder = new StringBuilderHolder(64);

    private QuoteExactMatcher(String expression, String searchStr) {
        super(expression, searchStr);
    }

    @Override
    public Set<SearchingPage> search(Set<SearchingPage> source) {
        return search(source, searchingPage -> ExactMatcher.find(searchingPage, getExpression(), getSearchStr()).getHitCount() > 0);
    }

    static class Parser extends AbstractTokenParser {

        public boolean accept(String input, int start) {
            if (input.charAt(start) != '"' || input.length() - start < 3) {
                return false;
            }

            StringBuilder stringBuilder = stringBuilderHolder.get();
            boolean escape = false;

            for (int i = start + 1; i < input.length(); i++) {
                char c = input.charAt(i);

                if (escape) {
                    // 只转义双引号，其余保存原样
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
                    if (i == input.length() - 1 || input.charAt(i + 1) <= ' ') {
                        String expression = input.substring(start, i + 1).toLowerCase();
                        String searchStr = stringBuilder.toString().toLowerCase();
                        token = new QuoteExactMatcher(expression, searchStr);
                        nextStart = i + 1;
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
