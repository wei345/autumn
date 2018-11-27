package xyz.liuw.autumn.search;

/**
 * @author liuwei
 * Created by liuwei on 2018/11/27.
 */
public abstract class AbstractMatcher implements Matcher {

    protected String expression;
    protected String expressionValue;

    public AbstractMatcher(String expression, String expressionValue) {
        this.expression = expression;
        this.expressionValue = expressionValue;
    }

    @Override
    public String getExpression() {
        return expression;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + ":" + getExpression() + ":" + expressionValue;
    }
}
