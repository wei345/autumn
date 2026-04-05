package io.liuwei.autumn.util;

import jakarta.annotation.Nullable;
import lombok.Getter;
import org.jspecify.annotations.NonNull;

import java.util.Iterator;
import java.util.function.Predicate;

import static org.apache.commons.lang3.StringUtils.*;

/**
 * @author liuwei
 * @since 2021-07-07 17:23
 */
public class LineReader implements Iterable<String> {
    private static final char NEWLINE = '\n';
    @Getter
    private final String text;
    private int pos; // default 0
    private int prevPos;

    public LineReader(String text) {
        this.text = text;
    }

    /**
     * @return next line of string, or null if there is no more
     */
    @Nullable
    public String nextLine() {
        String text = this.text;
        int pos = this.pos;

        if (pos >= text.length())
            return null;

        int end = text.indexOf(NEWLINE, pos);
        if (end == -1)
            end = text.length();
        else
            end++;

        String line = text.substring(pos, end);

        this.prevPos = pos;
        this.pos = end;
        return line;
    }

    public boolean hasNextLine() {
        return pos < text.length();
    }

    @Nullable
    public String nextNonBlankLine() {
        for (String line : this) {
            if (isNotBlank(line))
                return line;
        }
        return null;
    }

    /**
     * Returns a String of continuous lines starting with current position
     * in the original text, in which for each line <code>matcher</code>
     * returns true.
     *
     * @param matcher receive a line, return true for acceptance, or false for
     *                rejection and stop.
     * @return a String joined of continuous lines in the original text, from
     * current position to where <code>matcher</code> returns false or the end
     * of the text.
     */
    public String nextLinesAsString(Predicate<String> matcher) {
        StringBuilder sb = new StringBuilder();
        for (String line : this) {
            if (matcher.test(line))
                sb.append(line);
            else {
                back();
                break;
            }
        }
        return sb.toString();
    }

    /**
     * Returns a String of continuous lines from current position in the
     * original text to (excluded) where <code>end</code> returns true.
     *
     * @param end receive a line, return true for rejection and stop, false
     *            for acceptance.
     * @return a String of continuous lines in the original text from current
     * position to (excluded) where <code>end</code> return true.
     */
    public String nextLinesAsStringUntil(Predicate<String> end) {
        return nextLinesAsString(s -> !end.test(s));
    }

    public void back() {
        this.pos = this.prevPos;
    }

    public String remainingText() {
        String remaining = "";
        if (hasNextLine()) {
            int pos = this.pos;
            remaining = text.substring(pos);
            this.prevPos = pos;
            this.pos = text.length();
        }
        return remaining;
    }

    @Override
    public @NonNull Iterator<String> iterator() {
        return new Iterator<String>() {
            @Override
            public boolean hasNext() {
                return hasNextLine();
            }

            @Override
            public String next() {
                return nextLine();
            }
        };
    }
}
