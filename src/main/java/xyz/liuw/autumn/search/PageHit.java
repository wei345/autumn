package xyz.liuw.autumn.search;

import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * @author liuwei
 * Created by liuwei on 2018/11/27.
 */
public class PageHit {

    private List<Hit> pathHitList;
    private List<Hit> titleHitList;
    private List<Hit> bodyHitList;
    private int hitCount;
    private String preview;

    PageHit(@NotNull List<Hit> pathHitList,
            @NotNull List<Hit> titleHitList,
            @NotNull List<Hit> bodyHitList) {
        this.pathHitList = pathHitList;
        this.titleHitList = titleHitList;
        this.bodyHitList = bodyHitList;
        this.hitCount = pathHitList.size() + titleHitList.size() + bodyHitList.size();
    }

    public List<Hit> getPathHitList() {
        return pathHitList;
    }

    public List<Hit> getTitleHitList() {
        return titleHitList;
    }

    public List<Hit> getBodyHitList() {
        return bodyHitList;
    }

    @SuppressWarnings("WeakerAccess")
    public int getHitCount() {
        return hitCount;
    }

    public String getPreview() {
        return preview;
    }

    public void setPreview(String preview) {
        this.preview = preview;
    }

    @Override
    public String toString() {
        return hitCount + "|" + pathHitList.size() + "|" + titleHitList.size() + "|" + bodyHitList.size();
    }
}
