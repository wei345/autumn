package xyz.liuw.autumn.controller;

import com.vip.vjtools.vjkit.text.EscapeUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
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
import java.util.Map;

@Controller
public class MainController {

    @Autowired
    private DataService dataService;
    @Autowired
    private ResourceHttpRequestHandler resourceHttpRequestHandler;
    @Autowired
    private MediaService mediaService;
    @Autowired
    private PageService pageService;

    @RequestMapping(value = "/tree.json", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseBody
    public String treeJson(WebRequest webRequest) {
        TreeJson treeJson = dataService.getTreeJson();
        if (webRequest.checkNotModified(treeJson.getMd5())) {
            return null;
        }

        return treeJson.getJson();
    }

    @RequestMapping("/**")
    @ResponseBody
    public Object index(WebRequest webRequest,
                        HttpServletRequest request,
                        HttpServletResponse response,
                        Map<String, Object> model) throws ServletException, IOException {

        String path = EscapeUtil.urlDecode(request.getRequestURI());

        Media media = dataService.getMedia(path);
        if (media != null) {
            mediaService.output(media, webRequest, request, response);
            return null;
        }

        if (path.endsWith(".js") ||
                path.endsWith(".css") ||
                path.endsWith(".png") ||
                path.endsWith(".jpg")) {
            resourceHttpRequestHandler.handleRequest(request, response);
            return null;
        }

        Page page = dataService.getPage(path);
        if (page == null) {
            if (!SecurityService.isLogged() && dataService.pageExist(path)) {
                response.sendRedirect("/login?ret=" + path);
                return null;
            }
            response.sendError(HttpStatus.NOT_FOUND.value(), HttpStatus.NOT_FOUND.getReasonPhrase());
            return null;
        }
        return pageService.output(page, model, "main", webRequest);
    }

}