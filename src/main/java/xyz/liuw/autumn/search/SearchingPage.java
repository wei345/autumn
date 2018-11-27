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

    // MatcherExpression -> PageHit
    private Map<String, PageHit> hitMap;

    private int hitCount;

    public SearchingPage(Page page) {
        this.page = page;
        this.hitMap = Maps.newHashMapWithExpectedSize(3);
    }

    public Page getPage() {
        return page;
    }


    public PageHit getPageHit(String expression) {
        return hitMap.get(expression);
    }

    public void putPageHit(String expression, PageHit pageHit) {
        hitMap.put(expression, pageHit);
        updateHitCount();
    }

    private void updateHitCount() {
        int hitCount = 0;
        for (Map.Entry<String, PageHit> entry : hitMap.entrySet()) {
            hitCount += entry.getValue().getHitCount();
        }
        this.hitCount = hitCount;
    }

    public int getHitCount() {
        return hitCount;
    }

    @Override
    public String toString() {
        return page.getPath() + ":" + hitMap;
    }
}
