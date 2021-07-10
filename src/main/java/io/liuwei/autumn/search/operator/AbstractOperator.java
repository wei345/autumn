package io.liuwei.autumn.search.operator;

/**
 * @author liuwei
 * Created by liuwei on 2018/11/27.
 */
public abstract class AbstractOperator implements Operator {

    protected String expression;

    public AbstractOperator(String expression) {
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
