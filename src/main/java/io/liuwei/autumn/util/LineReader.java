package io.liuwei.autumn.util;

import lombok.Getter;

import java.util.Iterator;

/**
 * @author liuwei
 * @since 2021-07-07 17:23
 */
public class LineReader implements Iterable<String> {
    @Getter
    private final String text;
    private int start; // default 0
    private int prev;

    public LineReader(String text) {
        this.text = text;
    }

    private String readLine() {
        String text = this.text;
        int start = this.start;

        if (start >= text.length()) {
            return null;
        }

        int end = text.indexOf("\n", start);
        if (end == -1) {
            end = text.length();
        }

        String line = text.substring(start, end);

        this.prev = start;
        this.start = end + 1;
        return line;
    }

    public void back() {
        this.start = this.prev;
    }

    public String remainingText() {
        return text.substring(start);
    }

    @Override
    public Iterator<String> iterator() {
        return new Iterator<String>() {
            @Override
            public boolean hasNext() {
                return start < text.length();
            }

            @Override
            public String next() {
                return readLine();
            }
        };
    }
}
