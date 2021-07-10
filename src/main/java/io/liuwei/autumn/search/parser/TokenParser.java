package io.liuwei.autumn.search.parser;

import io.liuwei.autumn.search.Token;

/**
 * @author liuwei
 * Created by liuwei on 2018/11/27.
 */
public interface TokenParser {
    boolean accept(String input, int start);

    Token getToken();

    int getNextStart();
}
