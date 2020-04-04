package io.liuwei.autumn.search;

/**
 * @author liuwei
 * Created by liuwei on 2018/11/27.
 */
abstract class AbstractOperator implements Operator {

    protected String expression;

    AbstractOperator(String expression) {
        this.expression = expression;
    }

    @Override
    public String getExpression() {
        return expression;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + ":" + getExpression();
    }
}
