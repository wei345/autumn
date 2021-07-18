package io.liuwei.autumn.util;

import java.util.Collection;
import java.util.Iterator;

/**
 * @author liuwei
 * @since 2021-07-19 00:25
 */
public class CollectionUtil {

    /**
     * 不创建新的集合，返回一个 Iterator 对象，通过该对象可以依次遍历两个集合中的元素。
     */
    public static <T> Iterator<T> unionIterator(Collection<? extends T> coll1, Collection<? extends T> coll2) {
        Iterator<? extends T> iterator1 = coll1.iterator();
        Iterator<? extends T> iterator2 = coll2.iterator();
        return new Iterator<T>() {

            @Override
            public boolean hasNext() {
                return iterator1.hasNext() || iterator2.hasNext();
            }

            @Override
            public T next() {
                return iterator1.hasNext() ? iterator1.next() : iterator2.next();
            }
        };
    }
}
