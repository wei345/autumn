package xyz.liuw.autumn.search;

import xyz.liuw.autumn.data.Page;

import java.util.Set;

/**
 * @author liuwei
 * Created by liuwei on 2018/11/27.
 */
// Operator 结果
class ResultMatcher implements Matcher {
    private Set<Page> result;

    @Override
    public Set<Page> search() {
        return result;
    }

    @Override
    public Set<Page> search(Set<Page> source) {
        return result;
    }
}
