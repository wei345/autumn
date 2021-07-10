package io.liuwei.autumn.search;

import io.liuwei.autumn.model.Article;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.Stack;
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
        List<SearchingPage> sortedResult = Sorting.sort(searchResult);
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

}
