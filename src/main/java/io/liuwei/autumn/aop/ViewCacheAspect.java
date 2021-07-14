package io.liuwei.autumn.aop;

import io.liuwei.autumn.annotation.ViewCache;
import io.liuwei.autumn.model.RevisionContent;
import io.liuwei.autumn.model.ViewCacheLoader;
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
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.servlet.ModelAndView;

import java.util.Map;
import java.util.function.Supplier;

/**
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

    @Around("@annotation(vc)")
    public Object doAround(ProceedingJoinPoint joinPoint, ViewCache vc) throws Throwable {
        Object result = joinPoint.proceed();

        ServletRequestAttributes sra = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        ServletWebRequest webRequest = new ServletWebRequest(sra.getRequest(), sra.getResponse());
        String path = WebUtil.getInternalPath(sra.getRequest());

        if (result instanceof ViewCacheLoader) {
            ViewCacheLoader viewCacheLoader = (ViewCacheLoader) result;
            SimpleKey key = new SimpleKey(path, viewCacheLoader.getElements());
            return cacheable(key, viewCache, webRequest, () -> getModelAndView(viewCacheLoader, joinPoint));
        } else if (result instanceof String) {
            return cacheable(new SimpleKey(path), viewCache, webRequest, () -> new ModelAndView((String) result));
        }

        return result;
    }

    private ModelAndView getModelAndView(ViewCacheLoader viewCacheLoader, ProceedingJoinPoint joinPoint) {
        Object mv = viewCacheLoader.getLoader().get();

        if (mv instanceof ModelAndView) {
            return (ModelAndView) mv;
        } else if (mv instanceof String) {
            Map<String, Object> model = getModel(joinPoint);
            if (model == null) {
                return new ModelAndView((String) mv);
            } else {
                return new ModelAndView((String) mv, model);
            }
        } else {
            throw new IllegalArgumentException("期望 String 或 ModelAndView 类型. 实际 " +
                    (mv == null ? null : mv.getClass().getName()));
        }
    }

    private Map<String, Object> getModel(ProceedingJoinPoint joinPoint) {
        Class[] types = ((MethodSignature) joinPoint.getSignature()).getMethod().getParameterTypes();

        Map<String, Object> model = null;
        for (int i = 0; i < types.length; i++) {
            Class type = types[i];
            if (type == Model.class) {
                model = ((Model) joinPoint.getArgs()[i]).asMap();
                break;
            }
            if (type == Map.class) {
                //noinspection unchecked
                model = (Map<String, Object>) joinPoint.getArgs()[i];
                break;
            }
        }
        return model;
    }

    /**
     * 缓存视图渲染结果，之后的请求如果 key 相等，直接返回缓存里的数据，不重复渲染，提高性能。
     * <p>
     * 要正确缓存页面，需要检查模版访问的所有属性，当 key 相等时，这些属性也要相等。
     * 除了 key 覆盖的属性，还有一些 "全局" 属性，当它们发生变化时，要清除页面缓存。
     *
     * @param key        command 返回的 ModelAndView 的渲染结果会被放入缓存，关联这个 key
     * @param viewCache  缓存渲染结果
     * @param webRequest 用于检查和设置 ETag
     * @param loader     Controller handler 里处理该请求的逻辑，期望响应 200，返回 ModelAndView 对象
     * @return command 返回的 ModelAndView 对象，如果没有缓存；
     * 或 null，如果有缓存且 ETag 相等；
     * 或 ResponseEntity 对象，body 里为缓存的渲染结果，如果有缓存且 ETag 不相等。
     */
    private static Object cacheable(SimpleKey key, Cache viewCache, ServletWebRequest webRequest,
                                    Supplier<ModelAndView> loader) {
        RevisionContent rc = viewCache.get(key, RevisionContent.class);
        if (rc == null) {
            // Spring MVC 处理结束后，filter 会将渲染结果放入缓存
            ViewCacheFilter.enableContentCaching(webRequest.getRequest(), key);
            return loader.get();
        } else {
            if (webRequest.checkNotModified(rc.getEtag())) {
                return null;
            } else {
                return ResponseEntity
                        .status(HttpStatus.OK)
                        .contentType(rc.getMediaType())
                        .body(rc.getContent());
            }
        }
    }

}
