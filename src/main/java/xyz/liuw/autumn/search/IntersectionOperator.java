package xyz.liuw.autumn.search;

import xyz.liuw.autumn.data.Page;

import java.util.Set;

/**
 * 交集操作符。
 *
 * @author liuwei
 * Created by liuwei on 2018/11/27.
 */
class IntersectionOperator extends AbstractOperator {

    public IntersectionOperator(String expression) {
        super(expression);
    }

    @Override
    public int getPriority() {
        return 2;
    }

    @Override
    public Set<Page> operate(Matcher m1, Matcher m2) {
        return m2.search(m1.search());
    }

    static class Parser extends AbstractTokenParser {

        @Override
        public boolean accept(String input, int start) {

            if (input.charAt(start) > ' ') {
                return false;
            }

            int i = start + 1;
            while (i < input.length() && input.charAt(i) <= ' ') {
                i++;
            }

            nextStart = i;
            String exp = input.substring(start, i);
            token = new IntersectionOperator(exp);
            return true;
        }
    }
}
