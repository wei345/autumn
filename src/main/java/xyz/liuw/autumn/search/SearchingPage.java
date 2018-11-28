package xyz.liuw.autumn.search;

import com.google.common.collect.Maps;
import xyz.liuw.autumn.data.Page;

import java.util.Collections;
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
    private int bodyHitCount;

    private String pathPreview;
    private String titlePreview;
    private String bodyPreview;

    // h=a&h=b
    private String highlightString;

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
        int bodyHitCount = 0;
        for (Map.Entry<String, PageHit> entry : hitMap.entrySet()) {
            PageHit pageHit = entry.getValue();
            pathHitCount += pageHit.getPathHitList().size();
            titleHitCount += pageHit.getTitleHitList().size();
            bodyHitCount += pageHit.getBodyHitList().size();
        }
        this.pathHitCount = pathHitCount;
        this.titleHitCount = titleHitCount;
        this.bodyHitCount = bodyHitCount;
        this.hitCount = pathHitCount + titleHitCount + bodyHitCount;
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

    public int getBodyHitCount() {
        return bodyHitCount;
    }

    public Map<String, PageHit> getUnmodifiableHitMap() {
        return Collections.unmodifiableMap(hitMap);
    }

    public String getPathPreview() {
        return pathPreview;
    }

    public void setPathPreview(String pathPreview) {
        this.pathPreview = pathPreview;
    }

    public String getTitlePreview() {
        return titlePreview;
    }

    public void setTitlePreview(String titlePreview) {
        this.titlePreview = titlePreview;
    }

    public String getBodyPreview() {
        return bodyPreview;
    }

    public void setBodyPreview(String bodyPreview) {
        this.bodyPreview = bodyPreview;
    }

    public String getHighlightString() {
        return highlightString;
    }

    public void setHighlightString(String highlightString) {
        this.highlightString = highlightString;
    }

    @Override
    public String toString() {
        return page.getPath() + ":" + hitMap;
    }
}
