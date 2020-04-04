package io.liuwei.autumn.util;

import com.google.common.io.BaseEncoding;
import org.junit.Test;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author liuwei
 * Created by liuwei on 2019/1/11.
 */
public class BaseEncodingTest {

    @Test
    public void omitPadding(){

        String raw = "abcd";

        String base64 = BaseEncoding.base64Url().encode(raw.getBytes());
        assertThat(base64).contains("=");
        assertThat(new String(BaseEncoding.base64Url().decode(base64), UTF_8)).isEqualTo(raw);

        base64 = BaseEncoding.base64Url().omitPadding().encode(raw.getBytes());
        assertThat(base64).doesNotContain("=");
        assertThat(new String(BaseEncoding.base64Url().decode(base64), UTF_8)).isEqualTo(raw);
    }

}
