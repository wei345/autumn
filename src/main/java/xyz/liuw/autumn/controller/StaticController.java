package xyz.liuw.autumn.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.WebRequest;
import xyz.liuw.autumn.data.TreeJson;
import xyz.liuw.autumn.service.DataService;
import xyz.liuw.autumn.service.ResourceService;
import xyz.liuw.autumn.util.WebUtil;

/**
 * @author liuwei
 * Created by liuwei on 2018/12/6.
 */
@RestController
public class StaticController {

    static final String ALL_JS = "/js/all.js";

    static final String ALL_CSS = "/css/all.css";

    @Autowired
    private DataService dataService;

    @Autowired
    private ResourceService resourceService;

    @Autowired
    private WebUtil webUtil;

    // 这里定义的 mapping 不起作用，会进入优先级更高的 MainController "/**"
    @RequestMapping(value = ALL_JS, method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public Object allJs(WebRequest webRequest) {
        ResourceService.WebPageReferenceData jsCache = resourceService.getJsCache();
        if (webRequest.checkNotModified(jsCache.getEtag())) {
            return null;
        }
        return jsCache.getContent();
    }

    // 这里定义的 mapping 不起作用，会进入优先级更高的 MainController "/**"
    @RequestMapping(value = ALL_CSS, method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public Object allCss(WebRequest webRequest) {
        ResourceService.WebPageReferenceData cssCache = resourceService.getCssCache();
        if (webRequest.checkNotModified(cssCache.getEtag())) {
            return null;
        }
        return cssCache.getContent();
    }

    @RequestMapping(value = "/tree.json", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public String treeJson(WebRequest webRequest) {
        TreeJson treeJson = dataService.getTreeJson();

        String etag = treeJson.getEtag();
        if (etag == null) {
            //noinspection SynchronizationOnLocalVariableOrMethodParameter
            synchronized (treeJson) {
                if (treeJson.getEtag() == null) {
                    treeJson.setEtag(webUtil.getEtag(treeJson.getMd5()));
                }
            }
            etag = treeJson.getEtag();
        }

        if (webRequest.checkNotModified(etag)) {
            return null;
        }
        return treeJson.getJson();
    }

}
