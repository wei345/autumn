package io.liuwei.autumn.annotation;

import io.liuwei.autumn.aop.ViewCacheAspect;

import java.lang.annotation.*;


/**
 * 加在 Controller 方法上，并将返回值类型设为 Object，
 * 可缓存视图渲染结果，下次请求直接返回缓存，不重复渲染，提高性能。
 * <p>
 * 示例：
 * <pre><code>
 * {@literal @}ViewCache
 * {@literal @}GetMapping("/abc")
 *  public Object abc() {
 *     return "abc";
 *  }
 * </code></pre>
 *
 * @author liuwei
 * @see ViewCacheAspect
 * @since 2021-07-15 00:17
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface ViewCache {
}
