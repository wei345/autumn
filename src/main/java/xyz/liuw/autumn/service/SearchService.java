package xyz.liuw.autumn.service;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import xyz.liuw.autumn.data.Page;
import xyz.liuw.autumn.search.Highlighter;
import xyz.liuw.autumn.search.SearchResult;
import xyz.liuw.autumn.search.Searcher;

import java.util.List;
import java.util.Map;

/**
 * @author liuwei
 * Created by liuwei on 2018/11/26.
 */
@Component
public class SearchService {

    private static Logger logger = LoggerFactory.getLogger(SearchService.class);

    @Autowired
    private Searcher searcher;

    @Autowired
    private DataService dataService;

    private Highlighter highlighter = new Highlighter();

    public SearchResult search(String input, int offset, int count) {
        Map<String, Page> pageMap = dataService.getPageMap();
        SearchResult sr = searcher.search(input, pageMap.values(), offset, count);
        logger.info("Search '{}', {} results in {} ms", input, sr.getTotal(), sr.getTimeCost());
        return sr;
    }

    String highlightSearchStr(String html, List<String> searchStrList) {
        return highlighter.highlightSearchStr(html, searchStrList);
    }

}











