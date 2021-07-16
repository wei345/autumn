package io.liuwei.autumn.annotation;

import io.liuwei.autumn.aop.CheckModifiedAspect;

import java.lang.annotation.*;

/**
 * @author liuwei
 * @see CheckModifiedAspect
 * @since 2021-07-08
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface CheckModified {
}