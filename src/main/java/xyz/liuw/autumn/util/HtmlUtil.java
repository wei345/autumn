package xyz.liuw.autumn.util;

/**
 * @author liuwei
 * Created by liuwei on 2018/12/1.
 */
public class HtmlUtil {

    /**
     * 在给定的 html 中查找指定字符串的起始位置。相当于 HTML 版的 String#indexOf。
     * 忽略大小写，不会跨标签查找。
     * <pre><code>
     *     htmlIndexOf("ab<span>c</span>", "abc", 0)  // 返回 -1
     *     htmlIndexOf("ab<span></span>c", "abc", 0)  // 返回 -1
     *     htmlIndexOf("abc<span></span>", "abc", 0)  // 返回 0
     *     htmlIndexOf("<span>abc</span>", "abc", 0)  // 返回 6
     *     htmlIndexOf("<span>aBc</span>", "abc", 0)  // 返回 6
     * </code></pre>
     *
     * @param html   在 html 里查找 search。HTML 必须格式良好，< 和 > 必须成对出现
     * @param search 要在 html 中查找的字符串，必须已经 HTML 转义
     * @param start  html 起始（含）位置
     * @return 如果找到返回 >= 0 的整数，否则返回 -1
     */
    public static int indexOfIgnoreCase(String html, String search, int start) {
        if (html == null || search == null) {
            return -1;
        }
        if (search.length() == 0) {
            return 0;
        }
        int len = html.length();
        if (len - start < search.length()) {
            return -1;
        }
        search = search.toLowerCase();

        boolean inTag = false;
        char c;
        int j = 0;
        int maxJ = search.length() - 1;
        for (int i = start; i < len; i++) {
            c = html.charAt(i);
            if (c == '<') {
                inTag = true;
                continue;
            }
            if (c == '>') {
                j = 0;
                inTag = false;
                continue;
            }
            if (inTag) {
                continue;
            }
            if (Character.toLowerCase(c) == search.charAt(j)) {
                if (j == 0) {
                    start = i;
                }
                if (j == maxJ) {
                    return start;
                }
                j++;
            } else {
                j = 0;
            }
        }
        return -1;
    }
}
