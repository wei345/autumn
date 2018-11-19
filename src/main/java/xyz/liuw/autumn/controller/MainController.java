package xyz.liuw.autumn.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.resource.ResourceHttpRequestHandler;
import xyz.liuw.autumn.MarkdownParser;
import xyz.liuw.autumn.data.*;

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
    private DataSource dataSource;
    @Autowired
    private TemplateWatcher templateWatcher;
    @Autowired
    private MarkdownParser markdownParser;
    @Autowired
    private ResourceHttpRequestHandler resourceHttpRequestHandler;


    @RequestMapping(value = "/tree.json", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseBody
    public String treeJson(WebRequest webRequest) {
        TreeJson treeJson = dataSource.getTreeJson();
        if (webRequest.checkNotModified(treeJson.getMd5())) {
            return null;
        }

        return treeJson.getJson();
    }

    @RequestMapping("/**")
    public String index(WebRequest webRequest, HttpServletRequest request, HttpServletResponse response, Map<String, Object> model) throws ServletException, IOException {

        String path = request.getRequestURI();
        Page page = dataSource.getPage(path);

        if (page == null) {
            resourceHttpRequestHandler.handleRequest(request, response);
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
                String html = markdownParser.render(page.getBody());
                page.setBodyHtml(html);
            }
        }

        model.put("title", page.getTitle());
        model.put("body", page.getBodyHtml());
        return TPL_MAIN;
    }

}