package io.liuwei.autumn.search.matcher;

import io.liuwei.autumn.search.model.SearchingPage;
import org.apache.commons.lang3.Validate;

import javax.annotation.Nonnull;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author liuwei
 * Created by liuwei on 2018/11/27.
 */
public abstract class AbstractMatcher implements Matcher {

    private final String expression;

    private Set<SearchingPage> sourceData;

    AbstractMatcher(@Nonnull String expression) {
        Validate.notNull(expression, "Matcher expression is null");
        this.expression = expression;
    }

    protected abstract boolean test(SearchingPage searchingPage);

    @Override
    public Set<SearchingPage> search(Set<SearchingPage> source) {
        return source.stream()
                .filter(this::test)
                .collect(Collectors.toSet());
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
    public String toString() {
        return getClass().getSimpleName() + ":" + getExpression();
    }
}
