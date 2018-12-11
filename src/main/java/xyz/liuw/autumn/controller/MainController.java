package xyz.liuw.autumn.controller;

import com.google.common.collect.Maps;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.resource.ResourceHttpRequestHandler;
import org.springframework.web.servlet.view.RedirectView;
import xyz.liuw.autumn.data.Media;
import xyz.liuw.autumn.data.Page;
import xyz.liuw.autumn.service.DataService;
import xyz.liuw.autumn.service.MediaService;
import xyz.liuw.autumn.service.PageService;
import xyz.liuw.autumn.service.TemplateService;
import xyz.liuw.autumn.util.WebUtil;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

import static xyz.liuw.autumn.service.UserService.isLogged;

@RestController
public class MainController {

    private static final String MD_PATH_SUFFIX = ".md";
    private static final String PAGE_VIEW_NAME = "page";

    @Autowired
    private DataService dataService;

    @Autowired
    private ResourceHttpRequestHandler resourceHttpRequestHandler;

    @Autowired
    private MediaService mediaService;

    @Autowired
    private PageService pageService;

    @Autowired
    private StaticController staticController;

    @Autowired
    private TemplateService templateService;

    private Map<String, String> pathToView;

    @RequestMapping(value = "/**", method = RequestMethod.GET)
    public Object index(String[] h, // h=a&h=b..
                        WebRequest webRequest,
                        HttpServletRequest request,
                        HttpServletResponse response,
                        Map<String, Object> model) throws ServletException, IOException {

        String path = WebUtil.getRelativePath(request);
        templateService.setLogged(model);

        // Page
        DataService.SecurityBox pageBox = dataService.getPageSecurityBox(path);
        if (pageBox != null) {
            return handlePage(pageBox, path, false, h, webRequest, response, model);
        }

        // Page markdown source
        if (path.endsWith(MD_PATH_SUFFIX)) {
            String path2 = path.substring(0, path.length() - MD_PATH_SUFFIX.length());
            pageBox = dataService.getPageSecurityBox(path2);
            if (pageBox != null) {
                return handlePage(pageBox, path, true, h, webRequest, response, model);
            }
        }

        // Media
        Media media = dataService.getMedia(path);
        if (media != null) {
            return mediaService.output(media, webRequest, request, response);
        }

        if ("/js/all.js".equals(path)) {
            return staticController.allJs(webRequest);
        }

        if ("/css/all.css".equals(path)) {
            return staticController.cssJs(webRequest);
        }

        // 静态文件
        resourceHttpRequestHandler.handleRequest(request, response);
        return null;
    }

    private Object handlePage(DataService.SecurityBox pageBox,
                              String path,
                              boolean source,
                              String[] h,
                              WebRequest webRequest,
                              HttpServletResponse response,
                              Map<String, Object> model) throws IOException {

        Page page = pageBox.get();
        if (page != null) {
            String view = pathToView.get(path);
            if (view == null) {
                view = PAGE_VIEW_NAME;
            }

            if (source) {
                return pageService.getPageSource(page, webRequest);
            } else if (h != null && h.length > 0) {
                String[] ss = h;
                // 太多高亮词会影响性能，正常不会太多，可能是恶意请求
                if (ss.length > 10) {
                    ss = new String[10];
                    System.arraycopy(h, 0, ss, 0, ss.length);
                }
                return pageService.highlightOutput(page, Arrays.asList(ss), model, view);
            } else {
                return pageService.getPageContent(page, model, view, webRequest);
            }
        }
        // 无权限
        if (!isLogged()) {
            return new RedirectView("/login?ret=" + path, true, false);
        } else {
            response.sendError(403);
            return null;
        }
    }

    @Value("${autumn.path-to-view}")
    public void setPathToView(String str) {
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
}