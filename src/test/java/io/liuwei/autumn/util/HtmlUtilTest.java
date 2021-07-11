package io.liuwei.autumn.util;

import org.junit.Test;

import static io.liuwei.autumn.util.HtmlUtil.*;
import static org.junit.Assert.*;
import static org.springframework.web.util.HtmlUtils.*;

/**
 * @author liuwei
 * Created by liuwei on 2018/12/1.
 */
public class HtmlUtilTest {

    @Test
    public void testIndexOfIgnoreCase() {

        assertEquals(-1, indexOfIgnoreCase("ab<span>c</span>", "abc", 0));
        assertEquals(-1, indexOfIgnoreCase("ab<span></span>c", "abc", 0));
        assertEquals(0, indexOfIgnoreCase("abc<span></span>", "abc", 0));
        assertEquals(6, indexOfIgnoreCase("<span>abc</span>", "abc", 0));
        assertEquals(6, indexOfIgnoreCase("<span>aBc</span>", "abc", 0));

        assertEquals(-1, indexOfIgnoreCase("<span>a &lt; c</span>", "a < c", 0));
        assertEquals(6, indexOfIgnoreCase("<span>a &lt; c</span>", htmlEscape("a < c"), 0));
    }

    @Test
    public void makeNumberedToc() {
        String toHtml = "<div class=\"toc\">\n" +
                "<h3 class=\"no_selection action_toggle\">TOC</h3>\n" +
                "<ul>\n" +
                "<li><a href=\"#hbase\">HBase</a>\n" +
                "<ul>\n" +
                "<li><a href=\"#data-model\">data model</a></li>\n" +
                "<li><a href=\"#how-it-works\">how it works</a>\n" +
                "<ul>\n" +
                "<li><a href=\"#regions\">regions</a></li>\n" +
                "<li><a href=\"#meta-table\">META table</a></li>\n" +
                "<li><a href=\"#client-put-or-get-row\">client put or get row</a></li>\n" +
                "</ul>\n" +
                "</li>\n" +
                "<li><a href=\"#写优化\">写优化</a></li>\n" +
                "<li><a href=\"#ddl\">DDL</a></li>\n" +
                "<li><a href=\"#资料\">资料</a></li>\n" +
                "</ul>\n" +
                "</li>\n" +
                "</ul>\n" +
                "</div>";
        String tocHtml2 = HtmlUtil.makeNumberedToc(toHtml);
        System.out.println(tocHtml2);
    }
}