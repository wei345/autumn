package io.liuwei.autumn.search;

import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * @author liuwei
 * Created by liuwei on 2018/11/27.
 */
public class PageHit {

    private boolean nameEqual;
    private boolean titleEqual;
    private List<Hit> nameHitList;
    private List<Hit> pathHitList;
    private List<Hit> titleHitList;
    private List<Hit> bodyHitList;
    private int hitCount;

    PageHit(boolean nameEqual,
            boolean titleEqual,
            @NotNull List<Hit> nameHitList,
            @NotNull List<Hit> pathHitList,
            @NotNull List<Hit> titleHitList,
            @NotNull List<Hit> bodyHitList) {
        this.nameEqual = nameEqual;
        this.titleEqual = titleEqual;
        this.nameHitList = nameHitList;
        this.pathHitList = pathHitList;
        this.titleHitList = titleHitList;
        this.bodyHitList = bodyHitList;
        this.hitCount = pathHitList.size() + titleHitList.size() + bodyHitList.size();
    }

    boolean isNameEqual() {
        return nameEqual;
    }

    boolean isTitleEqual() {
        return titleEqual;
    }

    List<Hit> getNameHitList() {
        return nameHitList;
    }

    List<Hit> getPathHitList() {
        return pathHitList;
    }

    List<Hit> getTitleHitList() {
        return titleHitList;
    }

    List<Hit> getBodyHitList() {
        return bodyHitList;
    }

    @SuppressWarnings("WeakerAccess")
    public int getHitCount() {
        return hitCount;
    }

    @Override
    public String toString() {
        return hitCount + "|" + pathHitList.size() + "|" + titleHitList.size() + "|" + bodyHitList.size();
    }
}