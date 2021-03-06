package io.liuwei.autumn.search;

import io.liuwei.autumn.search.matcher.*;
import io.liuwei.autumn.search.operator.IntersectionOperator;
import io.liuwei.autumn.search.operator.UnionOperator;
import io.liuwei.autumn.search.parser.TokenParser;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author liuwei
 * Created by liuwei on 2018/11/27.
 */
public class InputParser {

    /**
     * 解析 input 返回 Token 列表。
     *
     * @param input 搜索字符串
     * @return Token 列表。确保第一个和最后一个不会是 Operator，因为它们在第一个或最后一个是没有意义的。
     */
    public List<Token> parse(String input) {
        if (StringUtils.isBlank(input)) {
            return Collections.emptyList();
        }

        List<Token> tokenList = new ArrayList<>(4);

        TokenParser[] parsers = new TokenParser[]{
                new WildcardQuoteMatcher.Parser(),
                new QuoteExactMatcher.Parser(),
                new CategoryMatcher.Parser(),
                new TagMatcher.Parser(),
                new ExcludeMatcher.Parser(),
                new ExactMatcher.Parser(),
                new UnionOperator.Parser(),
                new IntersectionOperator.Parser()
        };

        input = input.trim().replaceAll("\n", "");
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
