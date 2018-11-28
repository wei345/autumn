package xyz.liuw.autumn.search;

/**
 * 交集操作符。
 *
 * @author liuwei
 * Created by liuwei on 2018/11/27.
 */
class IntersectionOperator extends AbstractOperator {

    private IntersectionOperator(String expression) {
        super(expression);
    }

    @Override
    public int getPriority() {
        return 2;
    }

    @Override
    public Matcher operate(Matcher m1, Matcher m2) {
        // 不要 m2.search()，然后 m1.search()，然后取交集，那样效率低
        // 让一个 Matcher 在另一个 Matcher 结果中查找，效率更高
        return new ResultMatcher(m2.search(m1.search()));
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
