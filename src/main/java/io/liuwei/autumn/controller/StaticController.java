package io.liuwei.autumn.controller;

import io.liuwei.autumn.annotation.CheckModified;
import io.liuwei.autumn.aop.ViewRenderingCacheFilter;
import io.liuwei.autumn.constant.Constants;
import io.liuwei.autumn.service.StaticService;
import io.liuwei.autumn.util.MediaTypeUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.Cache;
import org.springframework.cache.interceptor.SimpleKey;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.servlet.ModelAndView;

import java.util.Map;

/**
 * @author liuwei
 * @since 2021-07-11 11:59
 */
@Controller
public class StaticController {

    @Autowired
    private StaticService staticService;

    @Autowired
    @Qualifier("viewCache")
    private Cache viewCache;

    @GetMapping(value = Constants.JS_ALL_DOT_JS, produces = MediaTypeUtil.TEXT_JAVASCRIPT_UTF8_VALUE)
    @ResponseBody
    @CheckModified
    public Object getAllJs() {
        return staticService.getAllJs();
    }

    @GetMapping(value = Constants.CSS_ALL_DOT_CSS, produces = MediaTypeUtil.TEXT_CSS_UTF8_VALUE)
    @ResponseBody
    @CheckModified
    public Object getAllCss() {
        return staticService.getAllCss();
    }

    @GetMapping(Constants.HELP)
    public Object help(ServletWebRequest webRequest, Map<String, Object> model) {
        return ViewRenderingCacheFilter.cacheable(
                new SimpleKey("/help"),
                viewCache,
                webRequest,
                () -> {
                    model.put("contentHtml", staticService.getHelpContent());
                    return new ModelAndView("content", model);
                });
    }
}
