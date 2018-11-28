package xyz.liuw.autumn.search;

abstract class AbstractTokenParser implements TokenParser {

    protected Token token;
    int nextStart;

    public abstract boolean accept(String input, int start);

    public Token getToken() {
        return token;
    }

    public int getNextStart() {
        return nextStart;
    }
}