package xyz.liuw.autumn.search;

import com.google.common.collect.Sets;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

/**
 * @author liuwei
 * Created by liuwei on 2018/11/28.
 */
class Sorting {
    private static final String ARCHIVE_PATH_PREFIX = "/archive/";

    static Supplier<Set<SearchingPage>> SET_SUPPLIER = Sets::newHashSet;

    static List<SearchingPage> sort(Set<SearchingPage> set) {
        List<SearchingPage> list = new ArrayList<>(set);
        list.sort((o1, o2) -> {
            // 一定要分出先后，也就是不能返回 0，否则每次搜索结果顺序可能不完全一样

            int v;

            // 非归档目录
            v = Integer.compare(o1.getPage().getPath().startsWith(Sorting.ARCHIVE_PATH_PREFIX) ? 1 : 0,
                    o2.getPage().getPath().startsWith(Sorting.ARCHIVE_PATH_PREFIX) ? 1 : 0);
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
            v = Long.compare(o2.getPage().getModified().getTime(), o1.getPage().getModified().getTime());
            if (v != 0) {
                return v;
            }

            // 字典顺序
            return o1.getPage().getPath().compareTo(o2.getPage().getPath());
        });
        return list;
    }
}
