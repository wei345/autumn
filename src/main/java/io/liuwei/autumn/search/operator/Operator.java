package io.liuwei.autumn.search.operator;

import io.liuwei.autumn.search.Token;
import io.liuwei.autumn.search.matcher.Matcher;

/**
 * @author liuwei
 * Created by liuwei on 2018/11/27.
 */
public interface Operator extends Token {
    /**
     * Operator 优先级。数值大的先执行。
     */
    int getPriority();

    Matcher operate(Matcher m1, Matcher m2);
}
