package xyz.liuw.autumn.search;

import xyz.liuw.autumn.data.Page;

import java.util.Set;

/**
 * @author liuwei
 * Created by liuwei on 2018/11/27.
 */
interface Operator extends Token {
    /**
     * 返回优先级。数值大的先执行。
     */
    int getPriority();

    Set<Page> operate(Matcher m1, Matcher m2);
}
