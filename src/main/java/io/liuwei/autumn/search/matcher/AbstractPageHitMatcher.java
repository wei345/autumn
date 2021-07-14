package io.liuwei.autumn.search.matcher;

import io.liuwei.autumn.model.Article;
import io.liuwei.autumn.search.model.Hit;
import io.liuwei.autumn.search.model.PageHit;
import io.liuwei.autumn.search.model.SearchingPage;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.interceptor.SimpleKey;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.function.Function;

/**
 * @author liuwei
 * Created by liuwei on 2018/12/13.
 */
public abstract class AbstractPageHitMatcher extends AbstractMatcher {
    private static Logger logger = LoggerFactory.getLogger(AbstractPageHitMatcher.class);

    @Getter
    @Setter
    private Cache hitCache;

    AbstractPageHitMatcher(@Nonnull String expression) {
        super(expression);
    }

    private PageHit cacheableFindPageHit(@Nonnull SearchingPage searchingPage,
                                         @Nonnull String expression,
                                         @Nonnull SimpleKey cacheKey,
                                         Function<String, List<Hit>> find) {
        Validate.notNull(searchingPage);
        Validate.notNull(expression);
        Validate.notNull(cacheKey);

        PageHit pageHit = searchingPage.getPageHit(cacheKey);
        if (pageHit == null) {
            pageHit = hitCache.get(cacheKey, () -> {
                Article page = searchingPage.getArticle();
                logger.debug("Searching page '{}' for expression '{}'", page.getPath(), expression);
                List<Hit> n = find.apply(page.getName());
                List<Hit> p = find.apply(page.getPath());
                List<Hit> t = find.apply(page.getTitle());
                List<Hit> b = find.apply(page.getContent());
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

    public PageHit getPageHit(SearchingPage searchingPage) {
        return cacheableFindPageHit(searchingPage, getExpression(), getPageHitCacheKey(searchingPage), this::getHitList);
    }

    public abstract List<Hit> getHitList(String source);

    public abstract SimpleKey getPageHitCacheKey(SearchingPage searchingPage);

}
