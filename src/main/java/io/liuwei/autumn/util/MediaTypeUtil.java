package io.liuwei.autumn.util;

import com.google.common.collect.Maps;
import com.vip.vjtools.vjkit.io.FileUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.util.StringUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Map;

/**
 * @author liuwei
 * Created by liuwei on 2018/12/6.
 */
@Slf4j
public class MediaTypeUtil {

    public static final String TEXT_HTML_UTF8_VALUE = "text/html;charset=UTF-8";

    public static final MediaType TEXT_HTML_UTF8 = MediaType.valueOf(TEXT_HTML_UTF8_VALUE);

    public static final String TEXT_JAVASCRIPT_UTF8_VALUE = "text/javascript;charset=UTF-8";

    public static final MediaType TEXT_JAVASCRIPT_UTF8 = MediaType.valueOf(TEXT_JAVASCRIPT_UTF8_VALUE);

    public static final String TEXT_CSS_UTF8_VALUE = "text/css;charset=UTF-8";

    public static final MediaType TEXT_CSS_UTF8 = MediaType.valueOf(TEXT_CSS_UTF8_VALUE);

    private static final String MIME_TYPES_FILE_NAME = "/org/springframework/http/mime.types";

    private static final MediaType DEFAULT_MIME_TYPE = MediaType.APPLICATION_OCTET_STREAM;

    private static final Map<String, MediaType> EXTENSION_2_MEDIA_TYPE_MAP;

    static {
        Map<String, MediaType> mediaTypeMap = parseMimeTypes();
        mediaTypeMap.put("adoc", MediaType.valueOf("text/asciidoc"));
        mediaTypeMap.put("asciidoc", MediaType.valueOf("text/asciidoc"));
        mediaTypeMap.put("pl", MediaType.valueOf("text/plain"));
        mediaTypeMap.put("sh", MediaType.valueOf("text/x-script.sh"));
        EXTENSION_2_MEDIA_TYPE_MAP = mediaTypeMap;
    }

    public static MediaType getMediaType(String filename) {
        return EXTENSION_2_MEDIA_TYPE_MAP.getOrDefault(FileUtil.getFileExtension(filename), DEFAULT_MIME_TYPE);
    }

    private static Map<String, MediaType> parseMimeTypes() {
        try (InputStream is = MediaTypeFactory.class.getResourceAsStream(MIME_TYPES_FILE_NAME)) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.US_ASCII));
            // 2/984 一个 fileExtension 对应 2 个 mediaType
            Map<String, MediaType> ext2MimeType = Maps.newHashMapWithExpectedSize(1024);

            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isEmpty() || line.charAt(0) == '#') {
                    continue;
                }
                String[] tokens = StringUtils.tokenizeToStringArray(line, " \t\n\r\f");
                MediaType mediaType = MediaType.valueOf(tokens[0]);
                for (int i = 1; i < tokens.length; i++) {
                    String fileExtension = tokens[i].toLowerCase(Locale.ENGLISH);
                    if (ext2MimeType.containsKey(fileExtension)) {
                        log.debug("{} 已存在，旧值 '{}'，替换为新值 '{}'",
                                fileExtension, ext2MimeType.get(fileExtension), mediaType.toString());
                    }
                    ext2MimeType.put(fileExtension, mediaType);
                }
            }

            return ext2MimeType;
        } catch (IOException ex) {
            throw new IllegalStateException("Could not load '" + MIME_TYPES_FILE_NAME + "'", ex);
        }
    }

}
