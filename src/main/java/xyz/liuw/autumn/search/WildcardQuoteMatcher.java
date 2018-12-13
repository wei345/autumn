package xyz.liuw.autumn.search;

import com.vip.vjtools.vjkit.text.StringBuilderHolder;
import org.springframework.lang.Nullable;

import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.apache.commons.lang3.StringUtils.indexOfIgnoreCase;
import static xyz.liuw.autumn.search.ExactMatcher.cacheableFindPageHit;

/**
 * <code>*</code> 可以匹配 0 个或多个任意字符。
 * 所以 <code>*</code> 在开头或末尾，搜索结果是一样的，只是 Hit 范围不一样，对用户来说只是高亮的词不一样。
 *
 * @author liuwei
 * Created by liuwei on 2018/12/12.
 */
public class WildcardQuoteMatcher implements Matcher {

    private Set<SearchingPage> sourceData;

    private String expression;
    private String[] searches;

    private WildcardQuoteMatcher(String expression, String[] searches) {
        this.expression = expression;
        this.searches = searches;
    }

    static PageHit findPageHit(@NotNull SearchingPage searchingPage, WildcardQuoteMatcher matcher) {
        return cacheableFindPageHit(
                searchingPage,
                matcher.getExpression(),
                matcher.getExpression(),
                s -> findHit(s, matcher.getSearches()));
    }

    static List<Hit> findHit(@Nullable String source, @Nullable String[] searches) {
        if (source == null || searches == null || searches.length == 0) {
            return Collections.emptyList();
        }
        List<Hit> hits = new ArrayList<>();
        int hitStart = 0, start = hitStart, found;
        for (; ; ) {
            for (int i = 0; i < searches.length; i++) {
                String search = searches[i];
                if (search.length() == 0) {
                    // 如果最后一个 search 是 *，则往后直到换行都加上
                    if (i == searches.length - 1) {
                        int j = source.indexOf("\n", start);
                        if (j > start) {
                            start = j;
                        } else if (j == -1) {
                            start = source.length();
                        }
                    }
                    continue;
                }

                if ((found = indexOfIgnoreCase(source, search, start)) == -1) {
                    return hits;
                }

                // 不要跨行。如果跨行，从行后位置重新查找
                int j = lastIndexOf(source, '\n', hitStart, found);
                if (j >= hitStart) {
                    hitStart = j + 1;
                    start = hitStart;
                    i = -1;
                    continue;
                }

                // 如果第一个 search 不是 *，hitStart 要往前挪
                if (i == 0) {
                    hitStart = found;
                }

                start = found + search.length();
            }

            hits.add(new Hit(hitStart, start, source.substring(hitStart, start)));
            hitStart = start;
        }
    }

    @SuppressWarnings("SameParameterValue")
    private static int lastIndexOf(String str, char search, int start, int end) {
        int index = -1;
        int len = Math.min(str.length(), end);
        for (int i = start; i < len; i++) {
            if (str.charAt(i) == search) {
                index = i;
            }
        }
        return index;
    }

    String[] getSearches() {
        return searches;
    }

    @Override
    public Set<SearchingPage> search(Set<SearchingPage> source) {
        return AbstractMatcher.search(
                source,
                searchingPage -> findPageHit(searchingPage, this).getHitCount() > 0);
    }

    @Override
    public String getSearchStr() {
        throw new UnsupportedOperationException();
    }


    @Override
    public void setSourceData(Set<SearchingPage> sourceData) {
        this.sourceData = sourceData;
    }

    @Override
    public Set<SearchingPage> getSourceData() {
        return sourceData;
    }

    @Override
    public Set<SearchingPage> search() {
        return search(sourceData);
    }

    @Override
    public String getExpression() {
        return expression;
    }

    static class Parser extends AbstractTokenParser {

        private static StringBuilderHolder stringBuilderHolder = new StringBuilderHolder(64);

        public boolean accept(String input, int start) {
            if (input.charAt(start) != '"' || input.length() - start < 4) { // 最短 "x*" 4 个字符
                return false;
            }

            List<String> searchStrList = new ArrayList<>(4);
            StringBuilder stringBuilder = stringBuilderHolder.get();
            boolean escape = false;
            boolean asterisk = false;
            int asteriskCount = 0;

            for (int i = start + 1; i < input.length(); i++) {
                char c = input.charAt(i);

                if (escape) {
                    // 只转义双引号和星号，其余保持原样
                    if (c == '"' || c == '*') {
                        stringBuilder.append(c);
                    } else {
                        stringBuilder.append('\\').append(c);
                    }
                    escape = false;
                    continue;
                }

                if (c == '\\') {
                    escape = true;
                    asterisk = false;
                    continue;
                }

                if (c == '*') {
                    if (asterisk) { // 连续 * 等于一个 *
                        continue;
                    }
                    asteriskCount++;
                    asterisk = true;
                    if (stringBuilder.length() > 0) {
                        searchStrList.add(stringBuilder.toString().toLowerCase());
                        stringBuilder.setLength(0);
                    }
                    searchStrList.add(""); // 用 "" 表示通配符 *
                    continue;
                }
                if (asterisk) {
                    asterisk = false;
                }

                if (c == '"') {
                    int nextIndex = i + 1;
                    if (nextIndex == input.length() || input.charAt(nextIndex) <= ' ') {
                        if (asteriskCount == 0) {
                            return false;
                        }

                        String expression = input.substring(start, nextIndex).toLowerCase();
                        if (stringBuilder.length() > 0) {
                            searchStrList.add(stringBuilder.toString().toLowerCase());
                            stringBuilder.setLength(0);
                        }

                        // 不能全是通配符，查找时会死循环
                        if (searchStrList.size() == asteriskCount) {
                            return false;
                        }

                        token = new WildcardQuoteMatcher(expression, searchStrList.toArray(new String[0]));
                        nextStart = nextIndex;
                        return true;
                    } else {
                        return false;
                    }
                }

                stringBuilder.append(c);
            }
            return false;
        }
    }


}