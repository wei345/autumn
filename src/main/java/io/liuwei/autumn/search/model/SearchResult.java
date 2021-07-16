package io.liuwei.autumn.search.model;

import lombok.Getter;

import java.util.List;

/**
 * @author liuwei
 * Created by liuwei on 2018/11/28.
 */
@Getter
public class SearchResult {

    private final List<SearchingPage> pages;
    private final long timeCost;
    private final int total;

    public SearchResult(List<SearchingPage> pages, long timeCost, int total) {
        this.pages = pages;
        this.timeCost = timeCost;
        this.total = total;
    }
}
