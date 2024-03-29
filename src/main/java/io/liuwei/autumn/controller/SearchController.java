package io.liuwei.autumn.controller;

import io.liuwei.autumn.annotation.ViewCache;
import io.liuwei.autumn.config.AppProperties;
import io.liuwei.autumn.constant.CacheKeys;
import io.liuwei.autumn.enums.AccessLevelEnum;
import io.liuwei.autumn.model.Pagination;
import io.liuwei.autumn.search.model.SearchResult;
import io.liuwei.autumn.service.SearchService;
import io.liuwei.autumn.util.RateLimiter;
import io.liuwei.autumn.util.WebUtil;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.view.RedirectView;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;

/**
 * @author liuwei
 * Created by liuwei on 2018/11/28.
 */
@SuppressWarnings("FieldCanBeLocal")
@Controller
@RequiredArgsConstructor
public class SearchController {

    private final int maxSearchStrLength = 120;

    private final SearchService searchService;

    @Autowired(required = false)
    private StringRedisTemplate stringRedisTemplate;

    private RateLimiter rateLimiter;

    private final AppProperties.Search search;

    @PostConstruct
    public void init() {
        this.rateLimiter = new RateLimiter(100, 600, 1_000_000, stringRedisTemplate);
    }

    @ViewCache
    @GetMapping("/search")
    public Object search(String q,
                         Integer offset,
                         AccessLevelEnum accessLevel,
                         Map<String, Object> model,
                         HttpServletRequest request,
                         HttpServletResponse response) throws IOException {
        if (StringUtils.isBlank(q)) {
            return new RedirectView("/", true, false);
        }

        if (offset == null || offset < 0) {
            offset = 0;
        }

        if (q.length() > maxSearchStrLength) {
            q = q.substring(0, maxSearchStrLength);
        }

        String rateKey = CacheKeys.RATE_LIMIT_SEARCH_PREFIX + WebUtil.getClientIpAddress(request);
        if (!rateLimiter.acquire(rateKey)) {
            response.sendError(HttpStatus.TOO_MANY_REQUESTS.value(), "稍后再试");
            return null;
        }

        int pageSize = search.getPageSize();

        SearchResult sr = searchService.search(q, accessLevel, offset, pageSize);

        String q1 = q;
        Pagination pagination = new Pagination(
                offset,
                pageSize,
                sr.getTotal(),
                (pageNumber, offset1) -> {
                    String url = "/search?q=" + q1;
                    if (offset1 > 0) {
                        url += "&offset=" + offset1;
                    }
                    return url;
                });
        model.put("sr", sr);
        model.put("pagination", pagination);
        return "search";
    }
}
