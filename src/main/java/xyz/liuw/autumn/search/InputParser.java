package xyz.liuw.autumn.search;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author liuwei
 * Created by liuwei on 2018/11/27.
 */
class InputParser {

    List<Token> parse(String input) {
        if (StringUtils.isBlank(input)) {
            return Collections.emptyList();
        }

        List<Token> tokenList = new ArrayList<>(4);

        TokenParser[] parsers = new TokenParser[]{
                new QuoteExactMatcher.Parser(),
                new CategoryMatcher.Parser(),
                new TagMatcher.Parser(),
                new ExcludeMatcher.Parser(),
                new ExactMatcher.Parser(),
                new UnionOperator.Parser(),
                new IntersectionOperator.Parser()
        };

        input = input.trim();
        int start = 0;
        while (start < input.length()) {
            boolean accepted = false;
            for (TokenParser parser : parsers) {
                if (parser.accept(input, start)) {
                    tokenList.add(parser.getToken());
                    start = parser.getNextStart();
                    accepted = true;
                    break;
                }
            }
            if (!accepted) {
                start++;
            }
        }

        return tokenList;
    }
}
