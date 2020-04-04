package io.liuwei.autumn.util;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.springframework.web.util.HtmlUtils.htmlEscape;
import static io.liuwei.autumn.util.HtmlUtil.indexOfIgnoreCase;

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
}