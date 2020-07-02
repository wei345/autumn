package io.liuwei.autumn;

import com.vip.vjtools.vjkit.security.CryptoUtil;
import com.vip.vjtools.vjkit.text.EncodeUtil;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author liuwei
 * Created by liuwei on 2018/11/23.
 */
public class AesTest {

    @Test
    public void generateKey() {
        // keysize: 128, 192, 256. 如果要使用 192 或 256 则需要安装 JCE
        byte[] bytes = CryptoUtil.generateAesKey(128);
        String key = EncodeUtil.encodeHex(bytes);

        System.out.println(key);
        validate(key);
    }

    private void validate(String keyStr) {
        String raw = UUID.randomUUID().toString();
        byte[] key = EncodeUtil.decodeHex(keyStr.toUpperCase());
        String encrypted = EncodeUtil.encodeBase64(CryptoUtil.aesEncrypt(raw.getBytes(StandardCharsets.UTF_8), key));
        String decrypted = CryptoUtil.aesDecrypt(EncodeUtil.decodeBase64(encrypted), key);
        assertThat(decrypted).isEqualTo(raw);
    }
}
