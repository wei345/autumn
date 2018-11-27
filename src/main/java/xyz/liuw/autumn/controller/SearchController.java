package xyz.liuw.autumn.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import xyz.liuw.autumn.search.SearchResult;
import xyz.liuw.autumn.service.SearchService;
import xyz.liuw.autumn.service.TemplateService;

import java.util.Map;

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

    @RequestMapping(value = "/search", method = RequestMethod.GET)
    public Object search(String s, Map<String, Object> model) {
        SearchResult sr = searchService.search(s);
        model.put("sr", sr);
        model.put("s", s);
        return templateService.merge(model, "search");
    }
}
