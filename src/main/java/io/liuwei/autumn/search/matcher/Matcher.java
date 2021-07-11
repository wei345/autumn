package io.liuwei.autumn.search.matcher;

import io.liuwei.autumn.search.model.SearchingPage;
import io.liuwei.autumn.search.Token;

import java.util.Set;

/**
 * @author liuwei
 * Created by liuwei on 2018/11/27.
 */
public interface Matcher extends Token {

    void setSourceData(Set<SearchingPage> sourceData);

    Set<SearchingPage> getSourceData();

    default Set<SearchingPage> search() {
        return search(getSourceData());
    }

    Set<SearchingPage> search(Set<SearchingPage> source);
}
