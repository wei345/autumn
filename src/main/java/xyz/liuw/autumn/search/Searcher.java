package xyz.liuw.autumn.search;

import org.springframework.stereotype.Component;
import xyz.liuw.autumn.data.Page;

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

    public SearchResult search(String input, Collection<Page> pages) {
        long start = System.currentTimeMillis();

        Set<SearchingPage> searchResult = doSearch(input, pages);
        List<SearchingPage> sortedResult = Sorting.sort(searchResult);
        highlighter.highlightSearchingPage(sortedResult);

        long cost = System.currentTimeMillis() - start;
        return new SearchResult(sortedResult, cost, pages.size());
    }

    private Set<SearchingPage> doSearch(String input, Collection<Page> pages) {
        Set<SearchingPage> sourceData = toSearchingPageSet(pages);

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

    private Set<SearchingPage> toSearchingPageSet(Collection<Page> all) {
        return all.stream().map(SearchingPage::new).collect(Collectors.toCollection(Sorting.SET_SUPPLIER));
    }

}
