package io.liuwei.autumn.aop;

import io.liuwei.autumn.annotation.ViewCache;
import io.liuwei.autumn.model.RevisionContent;
import io.liuwei.autumn.util.WebUtil;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.Cache;
import org.springframework.cache.interceptor.SimpleKey;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.ui.Model;
import org.springframework.ui.ModelMap;
import org.springframework.validation.BindingResult;
import org.springframework.validation.Errors;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.util.UriComponentsBuilder;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.http.PushBuilder;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Map;

/**
 * 缓存视图渲染结果，下次请求直接返回缓存，不重复渲染，提高性能。
 * <p>
 * <p><b>用法</b></p>
 *
 * <pre><code>
 * {@literal @}ViewCache
 * {@literal @}GetMapping("/abc")
 *  public Object abc() {
 *     return "abc";
 *  }
 * </code></pre>
 * <p>
 * 只需要在 Controller 方法上添加 {@link ViewCache} 注解，并将返回值类型设为 Object。
 *
 * <p><b>工作原理</b></p>
 * <p>
 * 通过 AOP Around 拦截 Controller 方法调用。
 * 构造缓存 key，读取缓存，如果命中，返回缓存结果，或 304 - 如果 ETag 相等。
 * 如果未命中，调用 Controller 方法，在 Spring MVC 处理完成后，通过 {@link ViewCacheFilter} 缓存响应内容。
 * <p>
 * 缓存 key 是自动生成的，组合了请求路径和方法参数。
 * <p>
 * 要正确缓存页面，需要检查模版文件访问的每个属性，确保当缓存 key 相等时，这些属性也相等。
 * <p>
 * {@link SettingModelAttributeInterceptor} 设置了一些 "全局" 属性，当它们发生变化时，需要清除所有页面缓存。
 *
 * @author liuwei
 * @since 2021-07-15 00:06
 */
@SuppressWarnings("ConstantConditions")
@Component
@Aspect
public class ViewCacheAspect {

    @Autowired
    @Qualifier("viewCache")
    private Cache viewCache;

    @Around("@annotation(viewCacheAnnotation)")
    public Object doAround(ProceedingJoinPoint joinPoint, ViewCache viewCacheAnnotation) throws Throwable {
        ServletRequestAttributes sra = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = sra.getRequest();
        HttpServletResponse response = sra.getResponse();

        SimpleKey key = getKey(request, joinPoint);
        RevisionContent rc = viewCache.get(key, RevisionContent.class);
        if (rc == null) {
            Object result = joinPoint.proceed();
            int statusCode = response.getStatus();
            if (statusCode >= 200 && statusCode < 300) {
                if (result instanceof String || result instanceof ModelAndView || result instanceof View) {
                    ViewCacheFilter.enableContentCaching(request, key);
                }
            }
            return result;
        } else {
            if (new ServletWebRequest(request, response).checkNotModified(rc.getEtag())) {
                return ResponseEntity
                        .status(HttpStatus.NOT_MODIFIED)
                        .build();
            } else {
                return ResponseEntity
                        .status(HttpStatus.OK)
                        .contentType(rc.getMediaType())
                        .body(rc.getContent());
            }
        }
    }

    /**
     * 构造缓存 key，组合请求路径和方法参数。
     */
    private SimpleKey getKey(HttpServletRequest request, ProceedingJoinPoint joinPoint) {
        ArrayList<Object> elements = new ArrayList<>(joinPoint.getArgs().length + 1);
        elements.add(WebUtil.getInternalPath(request));

        Class<?>[] types = ((MethodSignature) joinPoint.getSignature()).getMethod().getParameterTypes();
        Object[] args = joinPoint.getArgs();
        for (int i = 0; i < types.length; i++) {
            if (!isExcludeType(types[i])) {
                elements.add(args[i]);
            }
        }

        return new SimpleKey(elements.toArray());
    }

    /**
     * 这些类型不考虑 key 的一部分
     * see https://docs.spring.io/spring-framework/docs/current/reference/html/web.html#mvc-ann-arguments
     */
    private boolean isExcludeType(Class<?> type) {
        return Map.class.isAssignableFrom(type)
                || Model.class.isAssignableFrom(type)
                || ModelMap.class.isAssignableFrom(type)
                || Errors.class.isAssignableFrom(type)
                || BindingResult.class.isAssignableFrom(type)
                || RedirectAttributes.class.isAssignableFrom(type)
                || UriComponentsBuilder.class.isAssignableFrom(type)
                || WebRequest.class.isAssignableFrom(type)
                || ServletRequest.class.isAssignableFrom(type)
                || ServletResponse.class.isAssignableFrom(type)
                || HttpSession.class.isAssignableFrom(type)
                || PushBuilder.class.isAssignableFrom(type)
                || InputStream.class.isAssignableFrom(type)
                || Reader.class.isAssignableFrom(type)
                || OutputStream.class.isAssignableFrom(type)
                || Writer.class.isAssignableFrom(type);
    }
}
