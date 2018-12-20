package xyz.liuw.autumn.controller;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.view.RedirectView;
import xyz.liuw.autumn.search.SearchResult;
import xyz.liuw.autumn.service.RateLimitService;
import xyz.liuw.autumn.service.SearchService;
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

    @SuppressWarnings("FieldCanBeLocal")
    private int maxSearchStrLength = 120;

    @Autowired
    private RateLimitService rateLimitService;

    @RequestMapping(value = "/search", method = RequestMethod.GET)
    public Object search(String s, Map<String, Object> model, HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (StringUtils.isBlank(s)) {
            return new RedirectView("/", true, false);
        }

        if (s.length() > maxSearchStrLength) {
            s = s.substring(0, maxSearchStrLength);
        }

        if (rateLimitService.searchAcquire(WebUtil.getClientIpAddress(request))) {
            SearchResult sr = searchService.search(s);
            model.put("s", htmlEscape(s));
            model.put("sr", sr);
            return templateService.merge(model, "search_result");
        }
        request.setAttribute("s", htmlEscape(s));
        response.sendError(HttpStatus.TOO_MANY_REQUESTS.value(), "稍后再试");
        return null;
    }
}
