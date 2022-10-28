package io.liuwei.autumn.util;

import org.junit.jupiter.api.Test;

import java.util.regex.Matcher;

import static org.assertj.core.api.Assertions.*;

/**
 * @author liuwei
 * @since 2022-10-27 15:20
 */
class HttpURLConnectionUtilTest {

    @Test
    void CONTENT_TYPE_CHARSET_PARAM_PATTERN() {
        Matcher matcher;

        matcher = HttpURLConnectionUtil.CONTENT_TYPE_CHARSET_PARAM_PATTERN.matcher("text/html;charset=utf-8");
        assertThat(matcher.find()).isTrue();
        assertThat(matcher.group(1)).isEqualTo("utf-8");

        matcher = HttpURLConnectionUtil.CONTENT_TYPE_CHARSET_PARAM_PATTERN.matcher("Text/HTML;Charset=\"utf-8\"");
        assertThat(matcher.find()).isTrue();
        assertThat(matcher.group(1)).isEqualTo("utf-8");

        matcher = HttpURLConnectionUtil.CONTENT_TYPE_CHARSET_PARAM_PATTERN.matcher("text/html; charset=\"utf-8\"");
        assertThat(matcher.find()).isTrue();
        assertThat(matcher.group(1)).isEqualTo("utf-8");

        matcher = HttpURLConnectionUtil.CONTENT_TYPE_CHARSET_PARAM_PATTERN.matcher("text/html;charset=UTF-8");
        assertThat(matcher.find()).isTrue();
        assertThat(matcher.group(1)).isEqualTo("UTF-8");
    }

    @Test
    void CONTENT_TYPE_IS_TEXT_PATTERN() {
        Matcher matcher;
        matcher = HttpURLConnectionUtil.CONTENT_TYPE_IS_TEXT_PATTERN.matcher("text/html;charset=utf-8");
        assertThat(matcher.find()).isTrue();

        matcher = HttpURLConnectionUtil.CONTENT_TYPE_IS_TEXT_PATTERN.matcher("application/csvm+json");
        assertThat(matcher.find()).isTrue();

    }
}