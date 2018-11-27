package xyz.liuw.autumn.search;

import xyz.liuw.autumn.data.Page;

import java.util.Set;

/**
 * @author liuwei
 * Created by liuwei on 2018/11/27.
 */
interface Matcher extends Token {
    default MatcherContext getContext() {
        return null;
    }

    default String getPattern() {
        return null;
    }

    default Set<Page> search() {
        return search(getContext().getDatabase());
    }

    default Set<Page> search(Set<Page> source) {
        return null;
    }

    default String getExpression() {
        return null;
    }
}
