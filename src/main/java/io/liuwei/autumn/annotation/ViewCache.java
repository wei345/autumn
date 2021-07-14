package io.liuwei.autumn.annotation;

import java.lang.annotation.*;

/**
 * @author liuwei
 * @since 2021-07-15 00:17
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface ViewCache {
}
