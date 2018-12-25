package xyz.liuw.autumn.search;

/**
 * @author liuwei
 * Created by liuwei on 2018/11/27.
 */
interface Operator extends Token {
    /**
     * Operator 优先级。数值大的先执行。
     */
    int getPriority();

    Matcher operate(Matcher m1, Matcher m2);
}
