package io.liuwei.autumn.util;

import org.springframework.http.MediaType;
import org.springframework.util.DigestUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * @author liuwei
 * @since 2021-07-19 23:14
 */
@SuppressWarnings("WeakerAccess")
public class Md5Util {

    private static final byte[] CONTENT_TYPE_CONTENT_DELIMITER = {'\r', '\n'};

    public static String md5DigestAsHex(MediaType mediaType, byte[] content) {
        return md5DigestAsHex(
                mediaType.toString().getBytes(StandardCharsets.UTF_8),
                CONTENT_TYPE_CONTENT_DELIMITER,
                content);
    }

    public static String md5DigestAsHex(byte[]... arrays) {
        try {
            return DigestUtils.md5DigestAsHex(new MultiByteArrayInputStream(arrays));
        } catch (IOException e) {
            throw new IllegalStateException("Never to here");
        }
    }

}
