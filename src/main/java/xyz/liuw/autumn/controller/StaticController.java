package xyz.liuw.autumn.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.WebRequest;
import xyz.liuw.autumn.service.StaticService;

/**
 * @author liuwei
 * Created by liuwei on 2018/12/6.
 */
@RestController
public class StaticController {

    @Autowired
    private StaticService staticService;

    // 不要设置 produces，否则会进入优先级更高的 "/**"
    @RequestMapping(value = "/js/all.js", method = RequestMethod.GET)
    public Object allJs(WebRequest webRequest) {
        StaticService.WebPageReferenceData jsCache = staticService.getJsCache();
        if (webRequest.checkNotModified(jsCache.getEtag())) {
            return null;
        }
        return jsCache.getContent();
    }

    // 不要设置 produces，否则会进入优先级更高的 "/**"
    @RequestMapping(value = "/css/all.css", method = RequestMethod.GET)
    public Object allCss(WebRequest webRequest) {
        StaticService.WebPageReferenceData cssCache = staticService.getCssCache();
        if (webRequest.checkNotModified(cssCache.getEtag())) {
            return null;
        }
        return cssCache.getContent();
    }

}
