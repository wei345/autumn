package io.liuwei.autumn.search;

import com.google.common.collect.Maps;
import io.liuwei.autumn.data.Page;

import java.util.Collections;
import java.util.Map;

/**
 * @author liuwei
 * Created by liuwei on 2018/11/27.
 */
public class SearchingPage {

    private Page page;

    // Matcher expression -> PageHit
    private Map<String, PageHit> hitMap;

    private int hitCount; // default 0
    private int pathHitCount;
    private int titleHitCount;
    private int bodyHitCount;
    private int nameEqCount;
    private int titleEqCount;
    private int nameHitCount;

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
        // 即使 expression 重复也要累加 hit，排序更准
        updateHitCount(pageHit);
    }

    private void updateHitCount(PageHit pageHit) {
        if (pageHit.isNameEqual()) {
            nameEqCount++;
        }
        if (pageHit.isTitleEqual()) {
            titleEqCount++;
        }
        nameHitCount += pageHit.getNameHitList().size();
        pathHitCount += pageHit.getPathHitList().size();
        titleHitCount += pageHit.getTitleHitList().size();
        bodyHitCount += pageHit.getBodyHitList().size();
        hitCount = pathHitCount + titleHitCount + bodyHitCount;
    }

    @SuppressWarnings("WeakerAccess")
    public int getHitCount() {
        return hitCount;
    }

    int getNameHitCount() {
        return nameHitCount;
    }

    int getPathHitCount() {
        return pathHitCount;
    }

    int getTitleHitCount() {
        return titleHitCount;
    }

    public int getBodyHitCount() {
        return bodyHitCount;
    }

    Map<String, PageHit> getUnmodifiableHitMap() {
        return Collections.unmodifiableMap(hitMap);
    }

    public String getPathPreview() {
        return pathPreview;
    }

    void setPathPreview(String pathPreview) {
        this.pathPreview = pathPreview;
    }

    public String getTitlePreview() {
        return titlePreview;
    }

    void setTitlePreview(String titlePreview) {
        this.titlePreview = titlePreview;
    }

    public String getBodyPreview() {
        return bodyPreview;
    }

    void setBodyPreview(String bodyPreview) {
        this.bodyPreview = bodyPreview;
    }

    public String getHighlightString() {
        return highlightString;
    }

    void setHighlightString(String highlightString) {
        this.highlightString = highlightString;
    }

    int getNameEqCount() {
        return nameEqCount;
    }

    int getTitleEqCount() {
        return titleEqCount;
    }

    @Override
    public String toString() {
        return page.getPath() + ":" + hitMap;
    }
}
