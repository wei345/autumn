package io.liuwei.autumn.search;

import io.liuwei.autumn.model.Article;
import io.liuwei.autumn.search.matcher.AbstractPageHitMatcher;
import io.liuwei.autumn.search.matcher.Matcher;
import io.liuwei.autumn.search.model.SearchResult;
import io.liuwei.autumn.search.model.SearchingPage;
import io.liuwei.autumn.search.operator.Operator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.Cache;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author liuwei
 * Created by liuwei on 2018/11/27.
 */
@Component
public class Searcher {

    private final InputParser inputParser = new InputParser();

    private final Highlighter highlighter = new Highlighter();

    @Autowired
    @Qualifier("hitCache")
    private Cache hitCache;

    public SearchResult search(String input, Collection<Article> articles, int offset, int count) {
        long startTime = System.currentTimeMillis();

        Set<SearchingPage> searchResult = doSearch(input, articles);
        List<SearchingPage> sortedResult = sort(searchResult);
        int fromIndex = Math.min(offset, sortedResult.size());
        int toIndex = Math.min(offset + count, sortedResult.size());
        List<SearchingPage> result = sortedResult.subList(fromIndex, toIndex);
        highlighter.highlightSearchingPage(result);

        long cost = System.currentTimeMillis() - startTime;
        return new SearchResult(result, cost, sortedResult.size());
    }

    private Set<SearchingPage> doSearch(String input, Collection<Article> articles) {
        Set<SearchingPage> sourceData = toSearchingPage(articles);

        List<Token> tokenList = inputParser.parse(input);

        Stack<Matcher> operands = new Stack<>();
        Stack<Operator> operators = new Stack<>();
        for (Token token : tokenList) {
            if (token instanceof Matcher) {
                ((Matcher) token).setSourceData(sourceData);
                if (token instanceof AbstractPageHitMatcher) {
                    ((AbstractPageHitMatcher) token).setHitCache(hitCache);
                }
                operands.push((Matcher) token);
                continue;
            }
            if (token instanceof Operator) {
                Operator currOperator = (Operator) token;
                while (!operators.empty()) {
                    Operator prevOperator = operators.pop();
                    if (currOperator.getPriority() <= prevOperator.getPriority()) {
                        Matcher operand2 = operands.pop();
                        Matcher operand1 = operands.pop();
                        Matcher result = prevOperator.operate(operand1, operand2);
                        operands.push(result);
                    } else {
                        operators.push(prevOperator);
                        break;
                    }
                }
                operators.push(currOperator);
            }
        }

        while (!operators.empty()) {
            Operator operator = operators.pop();
            Matcher operand2 = operands.pop();
            Matcher operand1 = operands.pop();
            Matcher result = operator.operate(operand1, operand2);
            operands.push(result);
        }

        Matcher result = operands.pop();
        return result.search();
    }

    private Set<SearchingPage> toSearchingPage(Collection<Article> articles) {
        return articles
                .stream()
                .map(SearchingPage::new)
                .collect(Collectors.toSet());
    }

    private List<SearchingPage> sort(Set<SearchingPage> set) {
        List<SearchingPage> list = new ArrayList<>(set);
        list.sort(Comparator
                .comparing(SearchingPage::getNameEqCount).reversed()
                .thenComparing(SearchingPage::getTitleEqCount).reversed()
                .thenComparing(SearchingPage::getNameHitCount).reversed()
                .thenComparing(SearchingPage::getTitleHitCount).reversed()
                .thenComparing(SearchingPage::getPathHitCount).reversed()
                .thenComparing(SearchingPage::getHitCount).reversed()
                .thenComparing(o -> o.getArticle().getModified()).reversed()
                .thenComparing(o -> o.getArticle().getPath()));
        return list;
    }

}
