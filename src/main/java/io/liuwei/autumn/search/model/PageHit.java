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

    private final boolean nameEqual;
    private final boolean titleEqual;
    private final List<Hit> nameHitList;
    private final List<Hit> pathHitList;
    private final List<Hit> titleHitList;
    private final List<Hit> bodyHitList;
    private final int hitCount;

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
