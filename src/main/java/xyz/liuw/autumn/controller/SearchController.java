package xyz.liuw.autumn.controller;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import xyz.liuw.autumn.search.SearchResult;
import xyz.liuw.autumn.service.RateLimitService;
import xyz.liuw.autumn.service.SearchService;
import xyz.liuw.autumn.service.SecurityService;
import xyz.liuw.autumn.service.TemplateService;
import xyz.liuw.autumn.util.WebUtil;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;

import static org.springframework.web.util.HtmlUtils.htmlEscape;

/**
 * @author liuwei
 * Created by liuwei on 2018/11/28.
 */
@RestController
public class SearchController {

    @Autowired
    private SearchService searchService;
    @Autowired
    private TemplateService templateService;
    private int maxSearchStrLength = 120;
    @Autowired
    private RateLimitService rateLimitService;
    @Autowired
    private SecurityService securityService;

    @RequestMapping(value = "/search", method = RequestMethod.GET)
    public Object search(String s, Map<String, Object> model, HttpServletRequest request, HttpServletResponse response) throws IOException {
        securityService.setFreeMarkerLoggedKey(model);
        if (StringUtils.isBlank(s)) {
            response.sendRedirect("/");
            return null;
        }

        if (rateLimitService.acquireSearch(WebUtil.getClientIpAddress(request))) {
            if (s.length() > maxSearchStrLength) {
                s = s.substring(0, maxSearchStrLength);
            }
            SearchResult sr = searchService.search(s);
            model.put("sr", sr);
        } else {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            model.put("message", "稍后再试");
        }
        model.put("s", htmlEscape(s));
        return templateService.merge(model, "search");
    }
}
