package io.liuwei.autumn.controller;

import com.google.common.collect.Maps;
import io.liuwei.autumn.domain.Page;
import io.liuwei.autumn.service.StaticService;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.view.RedirectView;
import io.liuwei.autumn.domain.Media;
import io.liuwei.autumn.data.ResourceLoader;
import io.liuwei.autumn.domain.TreeJson;
import io.liuwei.autumn.service.DataService;
import io.liuwei.autumn.service.MediaService;
import io.liuwei.autumn.service.PageService;
import io.liuwei.autumn.util.WebUtil;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.constraints.NotNull;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

import static io.liuwei.autumn.service.DataService.LOGIN_REQUIRED_MEDIA;
import static io.liuwei.autumn.service.DataService.LOGIN_REQUIRED_PAGE;

@RestController
public class DefaultController {

    private static final String DOT_MD = ".md";
    private static final String DEFAULT_PAGE_VIEW = "page";

    @Autowired
    private DataService dataService;

    @Autowired
    private MediaService mediaService;

    @Autowired
    private PageService pageService;

    @Autowired
    private StaticService staticService;

    private Map<String, String> pathToView;

    @RequestMapping(value = "/js/all.js", method = RequestMethod.GET)
    public Object allJs(WebRequest webRequest) {
        return handleWebPageReferenceData(staticService.getJsCache(), webRequest);
    }

    @RequestMapping(value = "/css/all.css", method = RequestMethod.GET)
    public Object allCss(WebRequest webRequest) {
        return handleWebPageReferenceData(staticService.getCssCache(), webRequest);
    }

    private Object handleWebPageReferenceData(StaticService.WebPageReferenceData data, WebRequest webRequest) {
        if (WebUtil.checkNotModified(webRequest, data.getEtag())) {
            return null;
        }
        return data.getContent();
    }

    @RequestMapping(value = "/tree.json", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public String treeJson(WebRequest webRequest) {
        TreeJson treeJson = dataService.getTreeJson();

        if (WebUtil.checkNotModified(webRequest, treeJson.getEtag())) {
            return null;
        }

        return treeJson.getJson();
    }

    @RequestMapping(method = RequestMethod.GET)
    public Object index(String[] h, // h=a&h=b..
                        WebRequest webRequest,
                        HttpServletRequest request,
                        HttpServletResponse response,
                        Map<String, Object> model) throws IOException {

        String path = WebUtil.getInternalPath(request);

        // 静态文件
        ResourceLoader.ResourceCache resourceCache = staticService.getStaticResourceCache(path);
        if (resourceCache != null) {
            return staticService.handleRequest(resourceCache, webRequest, request, response);
        }

        // Page
        Page page = dataService.getPage(path);
        if (page != null) {
            return handlePage(page, path, false, h, webRequest, request, model);
        }

        // Page source
        if (path.endsWith(DOT_MD)) {
            String pathWithoutDotMd = path.substring(0, path.length() - DOT_MD.length());
            page = dataService.getPage(pathWithoutDotMd);
            if (page != null) {
                return handlePage(page, path, true, h, webRequest, request, model);
            }
        }

        // Media
        Media media = dataService.getMedia(path);
        if (media != null) {
            if (media == LOGIN_REQUIRED_MEDIA) {
                return redirectLoginView(path);
            }
            return mediaService.handleRequest(media, webRequest, request, response);
        }

        response.sendError(404);
        return null;
    }

    private Object handlePage(@NotNull Page page,
                              String path,
                              boolean source,
                              String[] h,
                              WebRequest webRequest,
                              HttpServletRequest request,
                              Map<String, Object> model) {
        Validate.notNull(page);

        if (page == LOGIN_REQUIRED_PAGE) {
            return redirectLoginView(path);
        }

        String view = pathToView.get(path);
        if (view == null) {
            view = DEFAULT_PAGE_VIEW;
        }

        if (source) {
            return pageService.handlePageSourceRequest(page, webRequest);
        }

        // highlight
        if (h != null && h.length > 0) {
            String[] strings = h;
            // 太多高亮词会影响性能，正常不会太多
            if (strings.length > 10) {
                strings = new String[10];
                System.arraycopy(h, 0, strings, 0, strings.length);
            }
            return pageService.handlePageHighlightRequest(page, Arrays.asList(strings), model, view, request);
        }

        return pageService.handlePageRequest(page, model, view, webRequest, request);
    }

    @Value("${autumn.path-to-view}")
    private void setPathToView(String str) {
        if (StringUtils.isBlank(str)) {
            this.pathToView = Collections.emptyMap();
            return;
        }

        String[] keyValuePairs = str.trim().split("\\s*,\\s*");
        Map<String, String> pathToView = Maps.newHashMapWithExpectedSize(keyValuePairs.length);
        Arrays.stream(keyValuePairs).forEach(s -> {
            String[] kv = s.split("\\s*=\\s*");
            pathToView.put(kv[0], kv[1]);
        });

        this.pathToView = pathToView;
    }

    private RedirectView redirectLoginView(String ret) {
        return new RedirectView("/login?ret=" + ret, true, false);
    }
}