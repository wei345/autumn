package io.liuwei.autumn.util;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author liuwei
 * @since 2021-07-18 21:36
 */
public class DiffUtil {

    public static <K, V> Diff<K> diff(Map<K, V> map1, Map<K, V> map2) {
        Diff<K> diff = new Diff<>(map2.size());

        for (Map.Entry<K, V> entry2 : map2.entrySet()) {
            K k2 = entry2.getKey();
            V v1 = map1.get(entry2.getKey());
            if (v1 == null) {
                diff.getAdded().add(k2);
            } else if (!v1.equals(entry2.getValue())) {
                diff.getModified().add(k2);
            } else {
                diff.getNotChanged().add(k2);
            }
        }

        for (Map.Entry<K, V> entry1 : map1.entrySet()) {
            if (!map2.containsKey(entry1.getKey())) {
                diff.getDeleted().add(entry1.getKey());
            }
        }

        return diff;
    }

    @Getter
    @Setter
    public static class Diff<T> {
        private List<T> added;
        private List<T> modified;
        private List<T> deleted;
        private List<T> notChanged;

        Diff(int count) {
            this.added = new ArrayList<>();
            this.modified = new ArrayList<>();
            this.deleted = new ArrayList<>();
            this.notChanged = new ArrayList<>(count);
        }
    }
}
