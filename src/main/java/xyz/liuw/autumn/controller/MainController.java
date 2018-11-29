package xyz.liuw.autumn.controller;

import com.vip.vjtools.vjkit.text.EscapeUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.resource.ResourceHttpRequestHandler;
import xyz.liuw.autumn.data.Media;
import xyz.liuw.autumn.data.Page;
import xyz.liuw.autumn.data.TreeJson;
import xyz.liuw.autumn.service.DataService;
import xyz.liuw.autumn.service.MediaService;
import xyz.liuw.autumn.service.PageService;
import xyz.liuw.autumn.service.SecurityService;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.Map;

@RestController
public class MainController {

    private static final String MD_PATH_SUFFIX = ".md";

    @Autowired
    private DataService dataService;
    @Autowired
    private ResourceHttpRequestHandler resourceHttpRequestHandler;
    @Autowired
    private MediaService mediaService;
    @Autowired
    private PageService pageService;

    @RequestMapping(value = "/tree.json", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public String treeJson(WebRequest webRequest) {
        TreeJson treeJson = dataService.getTreeJson();
        if (webRequest.checkNotModified(treeJson.getMd5())) {
            return null;
        }

        return treeJson.getJson();
    }

    @RequestMapping(value = "/**", method = RequestMethod.GET)
    public Object index(String[] h, // h=a&h=b..
                        WebRequest webRequest,
                        HttpServletRequest request,
                        HttpServletResponse response,
                        Map<String, Object> model) throws ServletException, IOException {

        String path = EscapeUtil.urlDecode(request.getRequestURI());

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
            mediaService.output(media, webRequest, request, response);
            return null;
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
            if (source) {
                return pageService.outputSource(page, webRequest);
            } else if (h != null && h.length > 0) {
                String[] ss = h;
                // 太多高亮词会影响性能，正常不会太多，可能是恶意请求
                if (ss.length > 10) {
                    ss = new String[10];
                    System.arraycopy(h, 0, ss, 0, ss.length);
                }
                return pageService.highlightOutput(page, Arrays.asList(ss), model, "main");
            } else {
                return pageService.output(page, model, "main", webRequest);
            }
        }
        // 无权限
        if (!SecurityService.isLogged()) {
            response.sendRedirect("/login?ret=" + path);
            return null;
        } else {
            response.sendError(403);
            return null;
        }
    }

}