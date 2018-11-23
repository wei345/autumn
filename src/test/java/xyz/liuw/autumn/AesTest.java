package xyz.liuw.autumn;

import com.vip.vjtools.vjkit.security.CryptoUtil;
import com.vip.vjtools.vjkit.text.EncodeUtil;
import org.junit.Test;
import org.springframework.util.DigestUtils;

import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author liuwei
 * Created by liuwei on 2018/11/23.
 */
public class AesTest {

    @Test
    public void test() {

        byte[] key = EncodeUtil.decodeHex("3729e4260d0361df9bafa807bc0da4a8".toUpperCase());
        String raw = "abc";

        String encrypted = EncodeUtil.encodeBase64(CryptoUtil.aesEncrypt(raw.getBytes(StandardCharsets.UTF_8), key));
        System.out.println(encrypted);

        String decrypted = CryptoUtil.aesDecrypt(EncodeUtil.decodeBase64(encrypted), key);

        assertThat(decrypted).isEqualTo(raw);

    }

    @Test
    public void generateKey() throws NoSuchAlgorithmException, InvalidKeySpecException {
        int size = 128; //128 192 256

        byte[] saltBytes = getSalt();
        SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
        PBEKeySpec spec = new PBEKeySpec(UUID.randomUUID().toString().toCharArray(), saltBytes, 65536, size);
        SecretKey secretKey = skf.generateSecret(spec);
        String key = EncodeUtil.encodeHex(secretKey.getEncoded());
        System.out.println(key);
    }

    private byte[] getSalt(){
        SecureRandom random = new SecureRandom();
        byte bytes[] = new byte[20];
        random.nextBytes(bytes);
        return bytes;
    }

}
