package io.liuwei.autumn.util;

import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author liuwei
 * @since 2022-02-28 13:51
 */
public class StringUtil {

    public static String replaceAll(String string, Pattern pattern, Function<Matcher, String> replacementFn) {
        StringBuilder stringBuilder = null;
        Matcher matcher = pattern.matcher(string);
        int start = 0;
        while (matcher.find()) {
            if (stringBuilder == null) {
                stringBuilder = new StringBuilder(string.length());
            }
            stringBuilder.append(string, start, matcher.start()).append(replacementFn.apply(matcher));
            start = matcher.end();
        }
        if (start == 0) {
            return string;
        }
        if (start < string.length()) {
            stringBuilder.append(string.substring(start));
        }
        return stringBuilder.toString();
    }

    public static String truncateUtf8Bytes(String str, int maxBytes) {
        if (str == null || str.length() * 3 <= maxBytes) {
            return str;
        }
        int byteArrayLen = 0;
        for (int i = 0, len = str.length(); i < len; i++) {
            char ch = str.charAt(i);

            // ranges from http://en.wikipedia.org/wiki/UTF-8
            int bytes;
            boolean skipNextChar = false;
            if (ch <= 0x007F) {
                bytes = 1;
            } else if (ch <= 0x07FF) {
                bytes = 2;
            } else if (ch <= 0xD7FF) {
                bytes = 3;
            } else if (ch <= 0xDFFF) {
                // surrogate area, consume next char as well
                bytes = 4;
                skipNextChar = true;
            } else {
                bytes = 3;
            }

            byteArrayLen += bytes;
            if (byteArrayLen > maxBytes) {
                return str.substring(0, i);
            }
            if (skipNextChar) {
                i++;
            }
        }
        return str;
    }

    /**
     * 如果字符串超过指定长度, 则截断, 并在末尾添加省略号 "..."
     *
     * <ul>
     *     <li>truncateAppendingEllipsis("abcdefg", 8) -> "abcdefg"</li>
     *     <li>truncateAppendingEllipsis("abcdefg", 7) -> "abcdefg"</li>
     *     <li>truncateAppendingEllipsis("abcdefg", 6) -> "abc..."</li>
     *     <li>truncateAppendingEllipsis("abcdefg", 5) -> "ab..."</li>
     *     <li>truncateAppendingEllipsis("abcdefg", 4) -> "a..."</li>
     *     <li>truncateAppendingEllipsis("abcdefg", 3) -> "..."</li>
     *     <li>truncateAppendingEllipsis("abcdefg", 2) -> throw IllegalArgumentException</li>
     *     <li>truncateAppendingEllipsis("abcdefg", 1) -> throw IllegalArgumentException</li>
     * </ul>
     *
     * @param str       可能超过指定长度的字符串, 可以为 null
     * @param maxLength >= 3
     * @return 原字符串或截取的字符串, 长度不超过 {@code maxLength}
     */
    public static String truncateAppendingEllipsis(String str, int maxLength) {
        if (str == null || str.length() <= maxLength) {
            return str;
        }
        String ellipsis = "...";
        int endIndex = maxLength - ellipsis.length();
        if (endIndex < 0) {
            throw new IllegalArgumentException("maxLength must >= " + ellipsis.length());
        }
        if (endIndex == 0) {
            return ellipsis;
        }
        char lastChar = str.charAt(endIndex - 1);
        // remove the trailing high surrogate
        if (lastChar >= 0xD800 && lastChar <= 0xDBFF) {
            endIndex--;
        }
        return str.substring(0, endIndex) + ellipsis;
    }
}
