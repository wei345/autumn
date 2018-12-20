package xyz.liuw.autumn.controller;

import com.google.common.collect.Maps;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.view.RedirectView;
import xyz.liuw.autumn.data.Media;
import xyz.liuw.autumn.data.Page;
import xyz.liuw.autumn.service.DataService;
import xyz.liuw.autumn.service.MediaService;
import xyz.liuw.autumn.service.PageService;
import xyz.liuw.autumn.service.StaticService;
import xyz.liuw.autumn.util.WebUtil;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.constraints.NotNull;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

import static xyz.liuw.autumn.controller.StaticController.ALL_CSS;
import static xyz.liuw.autumn.controller.StaticController.ALL_JS;
import static xyz.liuw.autumn.service.DataService.LOGIN_REQUIRED_MEDIA;
import static xyz.liuw.autumn.service.DataService.LOGIN_REQUIRED_PAGE;

@RestController
public class MainController {

    private static final String DOT_MD = ".md";
    private static final String DEFAULT_PAGE_VIEW = "page";

    @Autowired
    private DataService dataService;

    @Autowired
    private MediaService mediaService;

    @Autowired
    private PageService pageService;

    @Autowired
    private StaticController staticController;

    @Autowired
    private StaticService staticService;

    private Map<String, String> pathToView;

    @RequestMapping(value = "/**", method = RequestMethod.GET)
    public Object index(String[] h, // h=a&h=b..
                        WebRequest webRequest,
                        HttpServletRequest request,
                        HttpServletResponse response,
                        Map<String, Object> model) throws IOException {

        String path = WebUtil.getRelativePath(request);

        // Page
        Page page = dataService.getPage(path);
        if (page != null) {
            return handlePage(page, path, false, h, webRequest, response, model);
        }

        if (path.endsWith(DOT_MD)) {
            String pathWithoutDotMd = path.substring(0, path.length() - DOT_MD.length());
            page = dataService.getPage(pathWithoutDotMd);
            if (page != null) {
                return handlePage(page, path, true, h, webRequest, response, model);
            }
        }

        // Media
        Media media = dataService.getMedia(path);
        if (media != null) {
            if (media == LOGIN_REQUIRED_MEDIA) {
                return redirectLoginView(path);
            }
            return mediaService.output(media, webRequest, request, response);
        }

        if (ALL_JS.equals(path)) {
            return staticController.allJs(webRequest);
        }

        if (ALL_CSS.equals(path)) {
            return staticController.allCss(webRequest);
        }

        // 静态文件，也可以用 ResourceHttpRequestHandler
        return staticService.handleStaticRequest(path, webRequest, request, response);
    }

    private Object handlePage(@NotNull Page page,
                              String path,
                              boolean source,
                              String[] h,
                              WebRequest webRequest,
                              HttpServletResponse response,
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
            return pageService.getPageSource(page, webRequest);
        } else if (h != null && h.length > 0) {
            String[] ss = h;
            // 太多高亮词会影响性能，正常不会太多
            if (ss.length > 10) {
                ss = new String[10];
                System.arraycopy(h, 0, ss, 0, ss.length);
            }
            return pageService.highlightOutput(page, Arrays.asList(ss), model, view);
        } else {
            return pageService.getPageContent(page, model, view, webRequest);
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

    private RedirectView redirectLoginView(String ret) {
        return new RedirectView("/login?ret=" + ret, true, false);
    }
}