package io.liuwei.autumn.search;

/**
 * @author liuwei
 * Created by liuwei on 2018/11/27.
 */
interface Token {
    /**
     * @return 小写的 expression e.g. t:abc
     */
    String getExpression();
}
