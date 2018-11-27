package xyz.liuw.autumn.search;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

/**
 * @author liuwei
 * Created by liuwei on 2018/11/27.
 */
public class InputParserTest {

    @Test
    public void parse() {

        String input = "tag:java spring OR bash";
        InputParser parser = new InputParser();
        List<Token> tokenList = parser.parse(input);
        System.out.println(tokenList);

    }
}