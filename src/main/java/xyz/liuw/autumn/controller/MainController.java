package xyz.liuw.autumn.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.resource.ResourceHttpRequestHandler;
import xyz.liuw.autumn.data.Media;
import xyz.liuw.autumn.data.Page;
import xyz.liuw.autumn.data.TemplateWatcher;
import xyz.liuw.autumn.data.TreeJson;
import xyz.liuw.autumn.service.ContentService;
import xyz.liuw.autumn.service.DataService;
import xyz.liuw.autumn.service.MarkdownParser;
import xyz.liuw.autumn.service.SecurityService;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;

@Controller
public class MainController {

    private static final long CACHE_EXPIRE = System.currentTimeMillis();

    private static final String TPL_MAIN = "main";
    @Autowired
    private DataService dataService;
    @Autowired
    private TemplateWatcher templateWatcher;
    @Autowired
    private MarkdownParser markdownParser;
    @Autowired
    private ResourceHttpRequestHandler resourceHttpRequestHandler;
    @Autowired
    private ContentService contentService;

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
    public String index(WebRequest webRequest,
                        HttpServletRequest request,
                        HttpServletResponse response,
                        Map<String, Object> model) throws ServletException, IOException {

        String path = request.getRequestURI();

        Media media = dataService.getMedia(path);
        if (media != null) {
            contentService.output(media, webRequest, request, response);
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
                return "redirect:/login?ret=" + path;
            }
            response.setStatus(404);
            return null;
        }

        long lastModified = Math.max(CACHE_EXPIRE,
                Math.max(page.getLastModified(),
                        templateWatcher.getTemplateLastModified(TPL_MAIN)));

        if (webRequest.checkNotModified(lastModified)) {
            return null;
        }

        if (page.getBodyHtml() == null) {
            //noinspection SynchronizationOnLocalVariableOrMethodParameter
            synchronized (page) {
                if (page.getBodyHtml() == null) {
                    String html = markdownParser.render(page.getBody());
                    page.setBodyHtml(html);
                }
            }
        }

        model.put("title", page.getTitle());
        model.put("body", page.getBodyHtml());
        return TPL_MAIN;
    }

}