package io.liuwei.autumn.util;

import org.apache.commons.io.IOUtils;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author liuwei
 * @since 2021-07-19 23:42
 */
public class MultiByteArrayInputStreamTest {

    @Test
    public void read() throws IOException {
        String a = "abc";
        String b = "def";
        String c = "xyz";
        MultiByteArrayInputStream in = new MultiByteArrayInputStream(
                a.getBytes(StandardCharsets.UTF_8),
                b.getBytes(StandardCharsets.UTF_8),
                c.getBytes(StandardCharsets.UTF_8));
        assertThat(IOUtils.toString(in, StandardCharsets.UTF_8)).isEqualTo(a + b + c);
    }
}