package xyz.liuw.autumn.search;

import org.springframework.stereotype.Component;
import xyz.liuw.autumn.data.Page;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author liuwei
 * Created by liuwei on 2018/11/27.
 */
@Component
public class Searcher {

    private InputParser inputParser = new InputParser();

    public SearchResult search(String input, Collection<Page> all) {
        long start = System.currentTimeMillis();
        Set<SearchingPage> pages = search0(input, all);
        List<SearchingPage> list = sortByHitCount(pages);
        long cost = System.currentTimeMillis() - start;
        return new SearchResult(list, cost, all.size());
    }

    public Set<SearchingPage> search0(String input, Collection<Page> all) {
        Set<SearchingPage> data = toSearchingPageSet(all);

        List<Token> tokenList = inputParser.parse(input);
        Stack<Matcher> operands = new Stack<>();
        Stack<Operator> operators = new Stack<>();

        for (Token token : tokenList) {
            if (token instanceof Matcher) {
                ((Matcher) token).setSourceData(data);
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

    private List<SearchingPage> sortByHitCount(Set<SearchingPage> set) {
        List<SearchingPage> list = new ArrayList<>(set);
        list.sort((o1, o2) -> Integer.compare(o2.getHitCount(), o1.getHitCount()));
        return list;
    }

    private Set<SearchingPage> toSearchingPageSet(Collection<Page> all) {
        return all.stream().map(SearchingPage::new).collect(Collectors.toSet());
    }

}
