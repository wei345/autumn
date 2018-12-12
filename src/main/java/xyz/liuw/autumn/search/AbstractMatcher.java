package xyz.liuw.autumn.search;

import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xyz.liuw.autumn.data.Page;

import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * @author liuwei
 * Created by liuwei on 2018/11/27.
 */
abstract class AbstractMatcher implements Matcher {

    private static Logger logger = LoggerFactory.getLogger(AbstractMatcher.class);

    private String expression;
    private String searchStr;
    private Set<SearchingPage> sourceData;

    AbstractMatcher(@NotNull String expression, @NotNull String searchStr) {
        Validate.notNull(expression, "Matcher expression is null");
        Validate.notNull(searchStr, "Matcher searchStr is null");
        this.expression = expression;
        this.searchStr = searchStr;
    }

    static Set<SearchingPage> search(Set<SearchingPage> source, Predicate<SearchingPage> predicate) {
        return source.stream()
                .filter(predicate)
                .collect(Collectors.toCollection(Sorting.SET_SUPPLIER));
    }

    static PageHit cacheableFindPageHit(@NotNull SearchingPage searchingPage,
                                        @NotNull String expression,
                                        @NotNull String cacheKey,
                                        Function<String, List<Hit>> find) {
        Validate.notNull(searchingPage);
        Validate.notNull(expression);
        Validate.notNull(cacheKey);
        // 本次查询缓存
        PageHit pageHit = searchingPage.getPageHit(cacheKey);
        if (pageHit == null) {
            Page page = searchingPage.getPage();
            // Page 里的缓存
            ConcurrentHashMap<String, PageHit> searchStrToPageHit = page.getSearchHitCache();
            if (searchStrToPageHit == null) {
                //noinspection SynchronizationOnLocalVariableOrMethodParameter
                synchronized (page) {
                    if (page.getSearchHitCache() == null) {
                        page.setSearchHitCache(new ConcurrentHashMap<>(4));
                    }
                }
                searchStrToPageHit = page.getSearchHitCache();
            }
            pageHit = searchStrToPageHit.computeIfAbsent(cacheKey, s -> {
                logger.debug("Searching page '{}' for expression '{}'", page.getPath(), expression);
                List<Hit> n = find.apply(page.getName());
                List<Hit> p = find.apply(page.getPath());
                List<Hit> t = find.apply(page.getTitle());
                List<Hit> b = find.apply(page.getBody());
                Hit h;
                boolean nameEq = (n.size() == 1)
                        && (h = n.get(0)).getStart() == 0
                        && h.getEnd() == page.getName().length();
                boolean titleEq = (t.size() == 1)
                        && (h = t.get(0)).getStart() == 0
                        && h.getEnd() == page.getTitle().length();
                return new PageHit(nameEq, titleEq, n, p, t, b);
            });
            searchingPage.putPageHit(cacheKey, pageHit);
        }
        return pageHit;
    }

    @Override
    public Set<SearchingPage> getSourceData() {
        return sourceData;
    }

    @Override
    public void setSourceData(Set<SearchingPage> sourceData) {
        this.sourceData = sourceData;
    }

    @Override
    public String getExpression() {
        return expression;
    }

    @Override
    public String getSearchStr() {
        return searchStr;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + ":" + getExpression() + ":" + searchStr;
    }
}
