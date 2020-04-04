package io.liuwei.autumn.search;

import org.junit.Test;

import java.util.List;

/**
 * @author liuwei
 * Created by liuwei on 2018/11/27.
 */
public class InputParserTest {

    @Test
    public void parse() {
        InputParser parser = new InputParser();

        String input = "tag:java spring OR bash";
        List<Token> tokenList = parser.parse(input);
        System.out.println(tokenList);


        input = "tag:java spring OR ";
        tokenList = parser.parse(input);
        System.out.println(tokenList);

        input = "\"tag:java spring \\\"OR \"";
        tokenList = parser.parse(input);
        System.out.println(tokenList);

        input = "\"install success\" java";
        tokenList = parser.parse(input);
        System.out.println(tokenList);
    }
}