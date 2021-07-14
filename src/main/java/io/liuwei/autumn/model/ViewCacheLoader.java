package io.liuwei.autumn.model;

import lombok.Getter;

import java.util.function.Supplier;

/**
 * @author liuwei
 * @since 2021-07-14 22:57
 */
@Getter
public class ViewCacheLoader {
    private Supplier<Object> loader;
    private Object[] elements;

    /**
     * @param loader 返回 String 或 ModelAndView
     */
    public ViewCacheLoader(Supplier<Object> loader, Object... elements) {
        this.loader = loader;
        this.elements = elements;
    }
}
