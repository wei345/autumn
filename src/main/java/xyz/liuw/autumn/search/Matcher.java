package xyz.liuw.autumn.search;

import java.util.Set;

/**
 * @author liuwei
 * Created by liuwei on 2018/11/27.
 */
interface Matcher extends Token {

    default void setSourceData(Set<SearchingPage> sourceData) {
    }

    default Set<SearchingPage> getSourceData() {
        return null;
    }

    default Set<SearchingPage> search() {
        return search(getSourceData());
    }

    Set<SearchingPage> search(Set<SearchingPage> source);

    /**
     * @return 小写的 expression e.g. tag:abc
     */
    @Override
    default String getExpression() {
        return null;
    }

    /**
     * @return 小写的 searchStr e.g. abc
     */
    default String getSearchStr() {
        return null;
    }
}
