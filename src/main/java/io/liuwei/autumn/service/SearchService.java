package io.liuwei.autumn.service;


import io.liuwei.autumn.manager.ArticleManager;
import io.liuwei.autumn.enums.AccessLevelEnum;
import io.liuwei.autumn.model.Article;
import io.liuwei.autumn.search.Highlighter;
import io.liuwei.autumn.search.SearchResult;
import io.liuwei.autumn.search.Searcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

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
    private ArticleManager articleManager;

    private Highlighter highlighter = new Highlighter();

    public SearchResult search(String input, AccessLevelEnum accessLevel, int offset, int count) {
        List<Article> articles = articleManager.listArticles(accessLevel);
        SearchResult sr = searcher.search(input, articles, offset, count);
        logger.info("Search '{}', {} results in {} ms", input, sr.getTotal(), sr.getTimeCost());
        return sr;
    }

    public String highlightSearchStr(String html, List<String> searchStrList) {
        return highlighter.highlightSearchStr(html, searchStrList);
    }

}











