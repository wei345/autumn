package xyz.liuw.autumn.search;

import org.apache.commons.lang3.Validate;

import javax.validation.constraints.NotNull;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * @author liuwei
 * Created by liuwei on 2018/11/27.
 */
abstract class AbstractMatcher implements Matcher {

    private String expression;
    private String searchStr;
    private Set<SearchingPage> sourceData;

    AbstractMatcher(@NotNull String expression, @NotNull String searchStr) {
        Validate.notNull(expression, "Matcher expression is null");
        Validate.notNull(searchStr, "Matcher searchStr is null");
        this.expression = expression;
        this.searchStr = searchStr;
    }

    Set<SearchingPage> search(Set<SearchingPage> source, Predicate<SearchingPage> predicate) {
        return source.stream()
                .filter(predicate)
                .collect(Collectors.toCollection(Sorting.SET_SUPPLIER));
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
