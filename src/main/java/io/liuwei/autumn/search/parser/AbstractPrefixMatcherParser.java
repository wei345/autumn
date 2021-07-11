package io.liuwei.autumn.search.parser;

import io.liuwei.autumn.search.Token;
import org.apache.commons.lang3.Validate;

/**
 * 以指定前缀开头，到达末尾或遇到空格结束，并提取前缀和结束之间的部分。
 *
 * @author liuwei
 * Created by liuwei on 2018/11/27.
 */
public abstract class AbstractPrefixMatcherParser extends AbstractTokenParser {

    @Override
    public boolean accept(String input, int start) {
        return accept(getPrefix(), input, start);
    }

    private boolean accept(String prefix, String input, int start) {
        if (!input.startsWith(prefix, start) || // 在 start 位置非 prefix 开头
                input.length() - start == prefix.length() || // prefix 已到达末尾
                input.charAt(start + prefix.length()) <= ' ') { // prefix 之后是空格
            return false;
        }

        for (int i = start + prefix.length(); i < input.length(); i++) {
            char c = input.charAt(i);
            if (c <= ' ') {
                createMatcher(prefix, input, start, i);
                return true;
            }

            if (i == input.length() - 1) {
                createMatcher(prefix, input, start, i + 1);
                return true;
            }
        }
        //noinspection ConstantConditions
        Validate.isTrue(false, "不会到达这里");
        return false;
    }

    private void createMatcher(String prefix, String input, int start, int end) {
        String exp = input.substring(start, end).toLowerCase();
        String val = input.substring(start + prefix.length(), end).toLowerCase();
        token = createMatcher(exp, val);
        nextStart = end;
    }

    protected abstract String getPrefix();

    protected abstract Token createMatcher(String expression, String searchStr);
}
