package io.liuwei.autumn.search;

import com.vip.vjtools.vjkit.text.StringBuilderHolder;

import java.util.List;

/**
 * @author liuwei
 * Created by liuwei on 2018/11/27.
 */
class QuoteExactMatcher extends AbstractPageHitMatcher {

    private String searchStr;

    private QuoteExactMatcher(String expression, String searchStr) {
        super(expression);
        this.searchStr = searchStr;
    }

    @Override
    protected boolean test(SearchingPage searchingPage) {
        return getPageHit(searchingPage).getHitCount() > 0;
    }

    @Override
    List<Hit> getHitList(String source) {
        return ExactMatcher.findHitList(source, searchStr);
    }

    @Override
    String getPageHitCacheKey() {
        return searchStr;
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
