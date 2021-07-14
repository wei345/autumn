package io.liuwei.autumn.search.model;

import lombok.Getter;
import lombok.ToString;

import javax.annotation.Nonnull;
import java.util.List;

/**
 * @author liuwei
 * Created by liuwei on 2018/11/27.
 */
@Getter
@ToString
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
                   @Nonnull List<Hit> nameHitList,
                   @Nonnull List<Hit> pathHitList,
                   @Nonnull List<Hit> titleHitList,
                   @Nonnull List<Hit> bodyHitList) {
        this.nameEqual = nameEqual;
        this.titleEqual = titleEqual;
        this.nameHitList = nameHitList;
        this.pathHitList = pathHitList;
        this.titleHitList = titleHitList;
        this.bodyHitList = bodyHitList;
        this.hitCount = pathHitList.size() + titleHitList.size() + bodyHitList.size();
    }
}
