package xyz.liuw.autumn.search;

import java.util.Set;

/**
 * @author liuwei
 * Created by liuwei on 2018/11/27.
 */
interface Matcher extends Token {

    void setSourceData(Set<SearchingPage> sourceData);

    Set<SearchingPage> getSourceData();

    default Set<SearchingPage> search() {
        return search(getSourceData());
    }

    Set<SearchingPage> search(Set<SearchingPage> source);
}
