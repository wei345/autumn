package io.liuwei.autumn.service;


import io.liuwei.autumn.enums.AccessLevelEnum;
import io.liuwei.autumn.manager.ArticleManager;
import io.liuwei.autumn.model.Article;
import io.liuwei.autumn.search.Highlighter;
import io.liuwei.autumn.search.Searcher;
import io.liuwei.autumn.search.model.SearchResult;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @author liuwei
 * Created by liuwei on 2018/11/26.
 */
@Component
@Slf4j
public class SearchService {

    @Autowired
    private Searcher searcher;

    @Autowired
    private ArticleManager articleManager;

    private final Highlighter highlighter = new Highlighter();

    public SearchResult search(String input, AccessLevelEnum accessLevel, int offset, int count) {
        List<Article> articles = articleManager.listArticles(accessLevel);
        SearchResult sr = searcher.search(input, articles, offset, count);
        log.info("Search '{}', {} results in {} ms", input, sr.getTotal(), sr.getTimeCost());
        return sr;
    }

    public String highlightSearchStr(String html, List<String> searchStrList) {
        if (StringUtils.isBlank(html)) {
            return html;
        }
        return highlighter.highlightSearchStr(html, searchStrList);
    }

}











