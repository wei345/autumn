package io.liuwei.autumn.search;

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
        // 不要分别计算两个 Matcher 结果求并集，效率低。
        // 要先计算一个，另一个在第一个结果的补集里计算。
        // 如果其中一个是 ResultMatcher，先取它的结果。
        // 但是为了搜索结果排序和高亮，要找到所有 Hit，
        // 这跟分别计算两个 Matcher 结果求并集差不多了。
        return m1 instanceof ResultMatcher ? union(m1, m2) : union(m2, m1);
    }

    private Matcher union(Matcher m1, Matcher m2) {
        Set<SearchingPage> r1 = m1.search();
        Set<SearchingPage> r2 = m2.search(Sets.difference(m2.getSourceData(), r1));
        if (m2 instanceof ExactMatcher || m2 instanceof QuoteExactMatcher) {
            r1.forEach(((AbstractPageHitMatcher) m2)::getPageHit);
        }
        if (m2 instanceof WildcardQuoteMatcher) {
            r1.forEach(((WildcardQuoteMatcher) m2)::getPageHit);
        }
        return new ResultMatcher(Sets.union(r1, r2));
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
