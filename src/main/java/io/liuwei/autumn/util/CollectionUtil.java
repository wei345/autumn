package io.liuwei.autumn.util;

import java.util.Iterator;

/**
 * @author liuwei
 * @since 2021-07-19 00:25
 */
public class CollectionUtil {

    /**
     * 不创建新的集合，返回一个 Iterator 对象，通过该对象可以依次遍历多个集合中的元素。
     */
    @SafeVarargs
    public static <T> Iterator<T> unionIterator(Iterator<? extends T>... iterators) {

        return new Iterator<T>() {

            int index = 0;

            @Override
            public boolean hasNext() {
                for (; index < iterators.length; index++) {
                    if (iterators[index].hasNext()) {
                        return true;
                    }
                }
                return false;
            }

            @Override
            public T next() {
                return iterators[index].next();
            }
        };
    }
}
