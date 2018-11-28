package xyz.liuw.autumn.search;

import java.util.Set;

/**
 * @author liuwei
 * Created by liuwei on 2018/11/27.
 */
abstract class AbstractMatcher implements Matcher {

    private String expression;
    private String searchStr;
    private Set<SearchingPage> sourceData;

    AbstractMatcher(String expression, String searchStr) {
        this.expression = expression;
        this.searchStr = searchStr;
    }

    @Override
    public Set<SearchingPage> getSourceData() {
        return sourceData;
    }

    @Override
    public void setSourceData(Set<SearchingPage> sourceData) {
        this.sourceData = sourceData;
    }

    @Override
    public String getExpression() {
        return expression;
    }

    @Override
    public String getSearchStr() {
        return searchStr;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + ":" + getExpression() + ":" + searchStr;
    }
}
