package io.liuwei.autumn.search;

import com.google.common.collect.Maps;
import io.liuwei.autumn.domain.Page;
import io.liuwei.autumn.model.Article;
import lombok.Getter;
import lombok.Setter;

import java.util.Collections;
import java.util.Map;

/**
 * @author liuwei
 * Created by liuwei on 2018/11/27.
 */
@Getter
@Setter
public class SearchingPage {

    private Article article;

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

    public SearchingPage(Article article) {
        this.article = article;
        this.hitMap = Maps.newHashMapWithExpectedSize(3);
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

    Map<String, PageHit> getUnmodifiableHitMap() {
        return Collections.unmodifiableMap(hitMap);
    }

    @Override
    public String toString() {
        return article.getPath() + ":" + hitMap;
    }
}
