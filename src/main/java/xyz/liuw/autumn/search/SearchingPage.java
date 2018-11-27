package xyz.liuw.autumn.search;

import com.google.common.collect.Maps;
import xyz.liuw.autumn.data.Page;

import java.util.Map;

/**
 * @author liuwei
 * Created by liuwei on 2018/11/27.
 */
public class SearchingPage {

    private Page page;

    private Map<String, PageHit> hitMap;

    public SearchingPage(Page page) {
        this.page = page;
        this.hitMap = Maps.newHashMapWithExpectedSize(3);
    }

    public Page getPage() {
        return page;
    }

    public Map<String, PageHit> getHitMap() {
        return hitMap;
    }

    @Override
    public String toString() {
        return page.getPath() + ":" + getHitMap();
    }
}
