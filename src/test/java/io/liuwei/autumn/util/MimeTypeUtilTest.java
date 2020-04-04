package io.liuwei.autumn.util;

import org.junit.Test;
import org.springframework.http.MediaType;

import static org.assertj.core.api.Assertions.assertThat;
import static io.liuwei.autumn.util.MimeTypeUtil.getMimeType;

/**
 * @author liuwei
 * Created by liuwei on 2018/12/6.
 */
public class MimeTypeUtilTest {

    @Test
    public void testGetMimeType() {
        assertThat(getMimeType("a.sh")).isEqualTo("text/x-script.sh");
        assertThat(getMimeType("a.pl")).isNotEqualTo(MediaType.APPLICATION_OCTET_STREAM_VALUE);
        assertThat(getMimeType("a.txt")).isEqualTo("text/plain");
        assertThat(getMimeType("a.html")).isEqualTo("text/html");
    }
}