package io.liuwei.autumn.search.parser;

import io.liuwei.autumn.search.Token;

public abstract class AbstractTokenParser implements TokenParser {

    protected Token token;
    protected int nextStart;

    public abstract boolean accept(String input, int start);

    public Token getToken() {
        return token;
    }

    public int getNextStart() {
        return nextStart;
    }
}