package io.liuwei.autumn.search;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * @author liuwei
 * Created by liuwei on 2018/11/28.
 */
class Sorting {

    static List<SearchingPage> sort(Set<SearchingPage> set) {
        List<SearchingPage> list = new ArrayList<>(set);
        list.sort((o1, o2) -> {
            // 一定要分出先后，也就是不能返回 0，否则每次搜索结果顺序可能不完全一样

            int v;

            // 文件名相等
            v = Integer.compare(o2.getNameEqCount(), o1.getNameEqCount());
            if (v != 0) {
                return v;
            }

            // 标题相等
            v = Integer.compare(o2.getTitleEqCount(), o1.getTitleEqCount());
            if (v != 0) {
                return v;
            }

            // 文件名匹配
            v = Integer.compare(o2.getNameHitCount(), o1.getNameHitCount());
            if (v != 0) {
                return v;
            }

            // 标题匹配
            v = Integer.compare(o2.getTitleHitCount(), o1.getTitleHitCount());
            if (v != 0) {
                return v;
            }

            // 路径匹配
            v = Integer.compare(o2.getPathHitCount(), o1.getPathHitCount());
            if (v != 0) {
                return v;
            }

            // hit count 大在前
            v = Integer.compare(o2.getHitCount(), o1.getHitCount());
            if (v != 0) {
                return v;
            }

            // 最近修改日期
            v = Long.compare(o2.getArticle().getModified().getTime(), o1.getArticle().getModified().getTime());
            if (v != 0) {
                return v;
            }

            // 字典顺序
            return o1.getArticle().getPath().compareTo(o2.getArticle().getPath());
        });
        return list;
    }
}
