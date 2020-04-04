package io.liuwei.autumn.search;

import com.google.common.collect.Sets;

import java.util.Set;

/**
 * Operator 结果
 *
 * @author liuwei
 * Created by liuwei on 2018/11/27.
 */
class ResultMatcher implements Matcher {
    private Set<SearchingPage> result;

    ResultMatcher(Set<SearchingPage> result) {
        this.result = result;
    }

    @Override
    public void setSourceData(Set<SearchingPage> sourceData) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Set<SearchingPage> getSourceData() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Set<SearchingPage> search() {
        return result;
    }

    @Override
    public Set<SearchingPage> search(Set<SearchingPage> source) {
        return source.size() < result.size() ?
                Sets.intersection(source, result) :
                Sets.intersection(result, source);
    }

    @Override
    public String getExpression() {
        throw new UnsupportedOperationException();
    }

}
