package xyz.liuw.autumn.search;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.Nullable;
import xyz.liuw.autumn.util.HtmlUtil;

import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

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

    static PageHit find(@NotNull SearchingPage searchingPage,
                        @NotNull String expression,
                        @NotNull String searchStr) {
        // expression 和 searchStr 都可以作为 cacheKey，searchStr 命中率更高，
        // 因为结果 PageHit 由 searchStr 决定，而 xxx 和 -xxx searchStr 都是 xxx
        return cacheableFindPageHit(searchingPage, expression, searchStr, s -> find(s, searchStr));
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

    static List<Hit> htmlFind(@Nullable String source, @Nullable String search) {
        if (source == null || search == null) {
            return Collections.emptyList();
        }
        List<Hit> hits = new ArrayList<>();
        int i = 0, start;
        while ((start = HtmlUtil.indexOfIgnoreCase(source, search, i)) >= 0) {
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
