package xyz.liuw.autumn.search;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.Nullable;
import xyz.liuw.autumn.data.Page;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author liuwei
 * Created by liuwei on 2018/11/27.
 */
// abc
class ExactMatcher extends AbstractMatcher {
    private static Logger logger = LoggerFactory.getLogger(ExactMatcher.class);

    private ExactMatcher(String expression, String searchStr) {
        super(expression, searchStr);
    }

    static PageHit find(SearchingPage searchingPage, String matcherExpression, String searchStr) {
        // 本次查询缓存
        PageHit pageHit = searchingPage.getPageHit(matcherExpression);

        if (pageHit == null) {
            Page page = searchingPage.getPage();

            // Page 级缓存
            ConcurrentHashMap<String, PageHit> cache = page.getSearchHitCache();
            if (cache == null) {
                //noinspection SynchronizationOnLocalVariableOrMethodParameter
                synchronized (page) {
                    if (page.getSearchHitCache() == null) {
                        page.setSearchHitCache(new ConcurrentHashMap<>(4));
                    }
                }
                cache = page.getSearchHitCache();
            }
            pageHit = cache.get(matcherExpression);
            if (pageHit == null) {
                logger.info("Searching page {} for {}", page.getPath(), searchStr);
                List<Hit> p = find(page.getPath(), searchStr);
                List<Hit> t = find(page.getTitle(), searchStr);
                List<Hit> b = find(page.getBody(), searchStr);
                pageHit = new PageHit(p, t, b);
                cache.put(matcherExpression, pageHit);
            }
            searchingPage.putPageHit(matcherExpression, pageHit);
        }
        return pageHit;
    }

    @SuppressWarnings("WeakerAccess")
    static List<Hit> find(@Nullable String source, @Nullable String search) {
        if (source == null || search == null) {
            return Collections.emptyList();
        }
        List<Hit> hits = new ArrayList<>();
        int i = 0, start;
        while ((start = StringUtils.indexOfIgnoreCase(source, search, i)) >= 0) {
            i = start + search.length();
            hits.add(new Hit(start, i, search));
        }
        return hits;
    }

    @Override
    public Set<SearchingPage> search(Set<SearchingPage> source) {
        return search(source, searchingPage -> find(searchingPage, getExpression(), getSearchStr()).getHitCount() > 0);
    }

    static class Parser extends AbstractPrefixMatcherParser {

        @Override
        protected String getPrefix() {
            return "";
        }

        @Override
        protected Token createMatcher(String expression, String searchStr) {
            return new ExactMatcher(expression, searchStr);
        }
    }
}
