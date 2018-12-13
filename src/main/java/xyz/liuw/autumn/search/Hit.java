package xyz.liuw.autumn.search;

class Hit {
    private int start;
    private int end;
    private String str;

    Hit(int start, int end, String str) {
        this.start = start;
        this.end = end;
        this.str = str;
    }

    public int getStart() {
        return start;
    }

    public int getEnd() {
        return end;
    }

    String getStr() {
        return str;
    }
}