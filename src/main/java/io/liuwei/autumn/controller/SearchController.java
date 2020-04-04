package io.liuwei.autumn.controller;

import io.liuwei.autumn.domain.Pagination;
import io.liuwei.autumn.service.SearchService;
import io.liuwei.autumn.service.TemplateService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.view.RedirectView;
import io.liuwei.autumn.search.SearchResult;
import io.liuwei.autumn.service.RateLimitService;
import io.liuwei.autumn.util.WebUtil;

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

    @Value("${autumn.search.page-size}")
    private int pageSize;

    @RequestMapping(value = "/search", method = RequestMethod.GET)
    public Object search(String s,
                         Integer offset,
                         Map<String, Object> model,
                         HttpServletRequest request,
                         HttpServletResponse response) throws IOException {
        if (StringUtils.isBlank(s)) {
            return new RedirectView("/", true, false);
        }

        if (offset == null || offset < 0) {
            offset = 0;
        }

        if (s.length() > maxSearchStrLength) {
            s = s.substring(0, maxSearchStrLength);
        }

        if (rateLimitService.acquireSearch(WebUtil.getClientIpAddress(request))) {
            String q = s;
            SearchResult sr = searchService.search(s, offset, pageSize);
            model.put("s", htmlEscape(s));
            model.put("sr", sr);
            model.put("pagination",
                    new Pagination(
                            offset,
                            pageSize,
                            sr.getTotal(),
                            (pageNumber, offset1) -> "/search?s=" + q + "&offset=" + offset1));
            return templateService.merge(model, "search_result");
        }
        request.setAttribute("s", htmlEscape(s));
        response.sendError(HttpStatus.TOO_MANY_REQUESTS.value(), "稍后再试");
        return null;
    }
}
