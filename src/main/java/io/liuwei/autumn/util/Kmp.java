package io.liuwei.autumn.util;

import static java.lang.Character.*;

/**
 * KMP 字符串匹配算法，时间复杂度 O(m+n)，m，n 分别是 str 和 searchStr 的长度。
 *
 * @author liuwei
 * Created by liuwei on 2019/1/7.
 */
public class Kmp {

    /**
     * Finds the first index within a String using KMP algorithm
     *
     * @param str       the String to check
     * @param searchStr the String to find
     * @param startPos  the start position
     * @return the first index of the search String
     */
    public static int indexOfIgnoreCase(String str, String searchStr, int startPos) {
        if (str == null || searchStr == null) {
            return -1;
        }
        searchStr = searchStr.toLowerCase();
        int len = str.length();
        int searchStrLen = searchStr.length();
        int[] next = getNextArray(searchStr);
        int j = 0;
        for (int i = startPos; i < len; ++i) {
            char strChar = toLowerCase(str.charAt(i));
            char searchChar = searchStr.charAt(j);
            boolean isEquals;
            while (!(isEquals = strChar == searchChar) && j > 0) {
                j = next[j - 1] + 1;
                searchChar = searchStr.charAt(j);
            }
            if (isEquals) {
                ++j;
            }
            if (j == searchStrLen) {
                return i - searchStrLen + 1;
            }
        }
        return -1;
    }

    /**
     * @return "next 数组"，数组的下标是每个前缀结尾字符下标，
     * 数组的值是这个前缀的最长可以匹配前缀子串的结尾字符下标。
     */
    // 不必加缓存，即使 searchStr.length() == 80，加缓存也不会提高搜索速度。
    private static int[] getNextArray(String searchStr) {
        int len = searchStr.length();
        int[] next = new int[len];
        next[0] = -1;
        int k = -1; // 最大可匹配前缀子串结尾字符下标
        for (int i = 1; i < len; ++i) {
            // searchStr[0, i] 的最长可匹配前缀子串一定是 searchStr[0, i-1] 的某个可匹配前缀子串加上后面一个字符。
            // 我们由长到短查找 searchStr[0, i-1] 的每个可匹配前缀子串 searchStr[0, k]，
            // 考察 searchStr[k+1] 是否等于 searchStr[i]。
            // 次长可匹配子串，一定被包含在最长可匹配子串内，实际上它就是最长可匹配子串的最长可匹配子串。
            for (; ; ) {
                if (searchStr.charAt(k + 1) == searchStr.charAt(i)) {
                    ++k;
                    break;
                } else {
                    if (k == -1) {
                        break;
                    } else {
                        k = next[k];
                    }
                }
            }
            next[i] = k;
        }
        return next;
    }
}
