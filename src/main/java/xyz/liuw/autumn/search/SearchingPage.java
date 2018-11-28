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
    private int pathHitCount;
    private int titleHitCount;

    public SearchingPage(Page page) {
        this.page = page;
        this.hitMap = Maps.newHashMapWithExpectedSize(3);
    }

    public Page getPage() {
        return page;
    }


    PageHit getPageHit(String expression) {
        return hitMap.get(expression);
    }

    void putPageHit(String expression, PageHit pageHit) {
        hitMap.put(expression, pageHit);
        updateHitCount();
    }

    private void updateHitCount() {
        int pathHitCount = 0;
        int titleHitCount = 0;
        int hitCount = 0;
        for (Map.Entry<String, PageHit> entry : hitMap.entrySet()) {
            PageHit pageHit = entry.getValue();
            pathHitCount += pageHit.getPathHitList().size();
            titleHitCount += pageHit.getTitleHitList().size();
            hitCount += pageHit.getHitCount();
        }
        this.pathHitCount = pathHitCount;
        this.titleHitCount = titleHitCount;
        this.hitCount = hitCount;
    }

    @SuppressWarnings("WeakerAccess")
    public int getHitCount() {
        return hitCount;
    }

    public int getPathHitCount() {
        return pathHitCount;
    }

    public int getTitleHitCount() {
        return titleHitCount;
    }

    @Override
    public String toString() {
        return page.getPath() + ":" + hitMap;
    }
}
