package io.liuwei.autumn.search.model;

import lombok.Getter;

import java.util.List;

/**
 * @author liuwei
 * Created by liuwei on 2018/11/28.
 */
@Getter
public class SearchResult {

    private List<SearchingPage> pages;
    private long timeCost;
    private int total;

    public SearchResult(List<SearchingPage> pages, long timeCost, int total) {
        this.pages = pages;
        this.timeCost = timeCost;
        this.total = total;
    }
}
