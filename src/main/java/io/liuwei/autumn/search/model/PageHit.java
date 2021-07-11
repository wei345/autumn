package io.liuwei.autumn.search.model;

import lombok.Getter;

import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * @author liuwei
 * Created by liuwei on 2018/11/27.
 */
@Getter
public class PageHit {

    private boolean nameEqual;
    private boolean titleEqual;
    private List<Hit> nameHitList;
    private List<Hit> pathHitList;
    private List<Hit> titleHitList;
    private List<Hit> bodyHitList;
    private int hitCount;

    public PageHit(boolean nameEqual,
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

    @Override
    public String toString() {
        return hitCount + "|" + pathHitList.size() + "|" + titleHitList.size() + "|" + bodyHitList.size();
    }
}
