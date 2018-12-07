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

    // word1 word2。既包含 word1 又包含 word2 的 page
    // 完全匹配。"word1 word2"
    // 搜索通配符或未知字词。"word1 * word2"
    // 组合搜索。word1 OR word2。包含 word1 或 word2 的 page
    // 特定 tag 或 category。例如 tag:tag1 word1。包含 tag1 和 word1 的 page
    // 排除特定字词。-word1。不包含 word1 的 page

    @Autowired
    private Searcher searcher;

    @Autowired
    private DataService dataService;

    private Highlighter highlighter = new Highlighter();

    public SearchResult search(String input) {
        Map<String, Page> pageMap = dataService.getPageMap();
        SearchResult sr = searcher.search(input, pageMap.values());
        logger.info("Search '{}' got {} results in {} ms", input, sr.getPages().size(), sr.getTimeCost());
        return sr;
    }

    public String highlightSearchStr(String html, List<String> searchStrList) {
        return highlighter.highlightSearchStr(html, searchStrList);
    }

}











