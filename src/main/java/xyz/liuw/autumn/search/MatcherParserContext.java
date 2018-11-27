package xyz.liuw.autumn.search;

class MatcherParserContext {
    String input;
    int index; // default 0
    int length;

    char nextChar() {
        return (index < length) ? input.charAt(index++) : (char) 0;
    }

    public void setIndex(int index) {
        this.index = index;
    }
}