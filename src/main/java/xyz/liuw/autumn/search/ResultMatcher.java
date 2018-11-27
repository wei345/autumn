package xyz.liuw.autumn.search;

import com.google.common.collect.Sets;

import java.util.Set;

/**
 * @author liuwei
 * Created by liuwei on 2018/11/27.
 */
// Operator 结果
class ResultMatcher implements Matcher {
    private Set<SearchingPage> result;

    ResultMatcher(Set<SearchingPage> result) {
        this.result = result;
    }

    @Override
    public void setSourceData(Set<SearchingPage> sourceData) {
    }

    @Override
    public Set<SearchingPage> getSourceData() {
        return null;
    }

    @Override
    public Set<SearchingPage> search() {
        return result;
    }

    @Override
    public Set<SearchingPage> search(Set<SearchingPage> source) {
        return Sets.intersection(result, source);
    }

    @Override
    public String getExpression() {
        return null;
    }

    @Override
    public String getSearchStr() {
        return null;
    }
}
