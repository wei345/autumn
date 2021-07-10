package io.liuwei.autumn.search;

import io.liuwei.autumn.model.Article;
import io.liuwei.autumn.search.matcher.Matcher;
import io.liuwei.autumn.search.model.SearchResult;
import io.liuwei.autumn.search.model.SearchingPage;
import io.liuwei.autumn.search.operator.Operator;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author liuwei
 * Created by liuwei on 2018/11/27.
 */
@Component
public class Searcher {

    private InputParser inputParser = new InputParser();

    private Highlighter highlighter = new Highlighter();

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
        Set<SearchingPage> sourceData = toSearchingPageSet(articles);

        List<Token> tokenList = inputParser.parse(input);

        Stack<Matcher> operands = new Stack<>();
        Stack<Operator> operators = new Stack<>();
        for (Token token : tokenList) {
            if (token instanceof Matcher) {
                ((Matcher) token).setSourceData(sourceData);
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

    private Set<SearchingPage> toSearchingPageSet(Collection<Article> all) {
        return all.stream().map(SearchingPage::new).collect(Collectors.toSet());
    }

    private List<SearchingPage> sort(Set<SearchingPage> set) {
        List<SearchingPage> list = new ArrayList<>(set);
        list.sort((o1, o2) -> {
            // 一定要分出先后，也就是不能返回 0，否则每次搜索结果顺序可能不完全一样

            int v;

            // 文件名相等
            v = Integer.compare(o2.getNameEqCount(), o1.getNameEqCount());
            if (v != 0) {
                return v;
            }

            // 标题相等
            v = Integer.compare(o2.getTitleEqCount(), o1.getTitleEqCount());
            if (v != 0) {
                return v;
            }

            // 文件名匹配
            v = Integer.compare(o2.getNameHitCount(), o1.getNameHitCount());
            if (v != 0) {
                return v;
            }

            // 标题匹配
            v = Integer.compare(o2.getTitleHitCount(), o1.getTitleHitCount());
            if (v != 0) {
                return v;
            }

            // 路径匹配
            v = Integer.compare(o2.getPathHitCount(), o1.getPathHitCount());
            if (v != 0) {
                return v;
            }

            // hit count 大在前
            v = Integer.compare(o2.getHitCount(), o1.getHitCount());
            if (v != 0) {
                return v;
            }

            // 最近修改日期
            v = Long.compare(o2.getArticle().getModified().getTime(), o1.getArticle().getModified().getTime());
            if (v != 0) {
                return v;
            }

            // 字典顺序
            return o1.getArticle().getPath().compareTo(o2.getArticle().getPath());
        });
        return list;
    }

}
