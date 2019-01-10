package xyz.liuw.autumn.search;

import java.util.List;

/**
 * @author liuwei
 * Created by liuwei on 2018/11/28.
 */
public class SearchResult {

    private List<SearchingPage> pages;
    private long timeCost;
    private int total;

    SearchResult(List<SearchingPage> pages, long timeCost, int total) {
        this.pages = pages;
        this.timeCost = timeCost;
        this.total = total;
    }

    public List<SearchingPage> getPages() {
        return pages;
    }

    public long getTimeCost() {
        return timeCost;
    }

    public int getTotal() {
        return total;
    }
}
