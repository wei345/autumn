package io.liuwei.autumn.util;

import org.junit.Test;
import org.springframework.http.MediaType;

import static org.assertj.core.api.Assertions.*;

/**
 * @author liuwei
 * Created by liuwei on 2018/12/6.
 */
public class MimeTypeUtilTest {

    @Test
    public void testGetMimeType() {
        assertThat(MediaTypeUtil.getMediaType("a.sh")).isEqualTo(MediaType.valueOf("text/x-script.sh"));
        assertThat(MediaTypeUtil.getMediaType("a.txt")).isEqualTo(MediaType.valueOf("text/plain"));
        assertThat(MediaTypeUtil.getMediaType("a.html")).isEqualTo(MediaType.valueOf("text/html"));
        assertThat(MediaTypeUtil.getMediaType("a.pl")).isNotEqualTo(MediaType.APPLICATION_OCTET_STREAM);
    }
}