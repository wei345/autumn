package io.liuwei.autumn.search.parser;

import io.liuwei.autumn.search.Token;

public abstract class AbstractTokenParser implements TokenParser {

    protected Token token;
    protected int nextStart;

    @Override
    public abstract boolean accept(String input, int start);

    @Override
    public Token getToken() {
        return token;
    }

    @Override
    public int getNextStart() {
        return nextStart;
    }
}