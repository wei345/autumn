package xyz.liuw.autumn.search;

class Hit {
    private int start;
    private int end;

    Hit(int start, int end) {
        this.start = start;
        this.end = end;
    }

    public int getStart() {
        return start;
    }

    public int getEnd() {
        return end;
    }
}