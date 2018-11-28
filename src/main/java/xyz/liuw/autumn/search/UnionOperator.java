package xyz.liuw.autumn.search;

import com.google.common.collect.Sets;

import java.util.Set;

/**
 * 并集操作符。
 *
 * @author liuwei
 * Created by liuwei on 2018/11/27.
 */
class UnionOperator extends AbstractOperator {

    private UnionOperator(String expression) {
        super(expression);
    }

    @Override
    public int getPriority() {
        return 1;
    }

    @Override
    public Matcher operate(Matcher m1, Matcher m2) {
        Set<SearchingPage> a = m1.search();
        Set<SearchingPage> b = m2.search();
        Set<SearchingPage> c = Sets.newHashSetWithExpectedSize(a.size() + b.size());
        c.addAll(a);
        c.addAll(b);
        return new ResultMatcher(c);
    }

    // "a OR b"
    static class Parser extends AbstractTokenParser {

        private static final String OR = "OR";

        @Override
        public boolean accept(String input, int start) {
            // OR 之前要有一个或多个空格
            if (input.charAt(start) > ' ') {
                return false;
            }

            int i = start + 1;
            while (i < input.length() && input.charAt(i) <= ' ') {
                i++;
            }

            // OR
            if (!input.startsWith(OR, i)) {
                return false;
            }

            // OR 之后要有一个空格
            i = i + OR.length(); // OR 之后第一个字符索引
            if (i == input.length() || input.charAt(i) > ' ') {
                return false;
            }

            // 空格之后要有一个非空格字符
            for (i = i + 1; i < input.length(); i++) {
                if (input.charAt(i) > ' ') {
                    nextStart = i;
                    String exp = input.substring(start, i);
                    token = new UnionOperator(exp);
                    return true;
                }
            }

            return false;
        }
    }
}
