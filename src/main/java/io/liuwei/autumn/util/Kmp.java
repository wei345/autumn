package io.liuwei.autumn.util;

import static java.lang.Character.toLowerCase;

/**
 * KMP 字符串匹配算法，时间复杂度 O(m+n)，m，n 分别是主串和模式串的长度。
 *
 * @author liuwei
 * Created by liuwei on 2019/1/7.
 */
public class Kmp {

    /**
     * 用 KMP 算法在主串中查找模式串。
     *
     * @param a        主串
     * @param b        模式串，小写
     * @param startPos 从主串的这个位置开始查找
     * @return 主串中第一个模式串的起始索引
     */
    public static int kmpIgnoreCase(String a, String b, int startPos) {
        int n = a.length();
        int m = b.length();
        int[] next = getNexts(b);
        int j = 0;
        for (int i = startPos; i < n; ++i) {
            char ac = a.charAt(i), bc;
            boolean isEquals;
            while (!(isEquals = (ac == (bc = b.charAt(j)) || toLowerCase(ac) == bc)) && j > 0) { // 一直找到 a[i] 和 b[j]
                j = next[j - 1] + 1;
            }
            if (isEquals) {
                ++j;
            }
            if (j == m) { // 找到匹配模式串的了
                return i - m + 1;
            }
        }
        return -1;
    }

    /**
     * @param b 模式串
     */
    // 不必加缓存，即使 b.length() == 80，加缓存也不会提高搜索速度。
    private static int[] getNexts(String b) {
        int m = b.length();
        int[] next = new int[m];
        next[0] = -1;
        int k = -1;
        for (int i = 1; i < m; ++i) {
            while (k != -1 && b.charAt(k + 1) != b.charAt(i)) {
                k = next[k];
            }
            if (b.charAt(k + 1) == b.charAt(i)) {
                ++k;
            }
            next[i] = k;
        }
        return next;
    }
}
