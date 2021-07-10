package io.liuwei.autumn.util;

import com.google.common.collect.Maps;
import com.vip.vjtools.vjkit.io.FileUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
public class MimeTypeUtil {

    public static final String TEXT_JAVASCRIPT_UTF8 = "text/javascript;charset=UTF-8";
    public static final String TEXT_CSS_UTF8 = "text/css;charset=UTF-8";
    private static final String MIME_TYPES_FILE_NAME = "/org/springframework/http/mime.types";
    private static final String DEFAULT_MIME_TYPE = MediaType.APPLICATION_OCTET_STREAM_VALUE;
    private static Logger logger = LoggerFactory.getLogger(MimeTypeUtil.class);
    private static Map<String, String> fileExtensionToMimeType;
    private static Map<String, MediaType> mimeType2MediaType;

    static {
        setMimeTypes();
    }

    public static String getMimeType(String filename) {
        String v = fileExtensionToMimeType.get(FileUtil.getFileExtension(filename));
        return (v == null ? DEFAULT_MIME_TYPE : v);
    }

    public static MediaType getMediaType(String filename) {
        MediaType mediaType = mimeType2MediaType.get(getMimeType(filename));
        return mediaType == null ? MediaType.APPLICATION_OCTET_STREAM : mediaType;
    }

    public static MediaType getMediaTypeByMimeType(String mimeType) {
        MediaType mediaType = mimeType2MediaType.get(mimeType);
        return mediaType == null ? MediaType.valueOf(mimeType) : mediaType;
    }

    private static void setMimeTypes() {
        Map<String, String> mimeTypeMap = parseMimeTypes();
        mimeTypeMap.put("adoc", "text/asciidoc");
        mimeTypeMap.put("asciidoc", "text/asciidoc");
        mimeTypeMap.put("pl", "text/plain");
        mimeTypeMap.put("sh", "text/x-script.sh");
        fileExtensionToMimeType = mimeTypeMap;

        Map<String, MediaType> mimeTypeToMediaType = Maps.newHashMapWithExpectedSize(mimeTypeMap.size());
        for (Map.Entry<String, String> entry : mimeTypeMap.entrySet()) {
            if (!mimeTypeToMediaType.containsKey(entry.getValue())) {
                mimeTypeToMediaType.put(entry.getValue(), MediaType.valueOf(entry.getValue()));
            }
        }
        MimeTypeUtil.mimeType2MediaType = mimeTypeToMediaType;
    }

    private static Map<String, String> parseMimeTypes() {
        try (InputStream is = MediaTypeFactory.class.getResourceAsStream(MIME_TYPES_FILE_NAME)) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.US_ASCII));
            // 2/984 一个 fileExtension 对应 2 个 mediaType
            Map<String, String> ext2MimeType = Maps.newHashMapWithExpectedSize(1024);

            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isEmpty() || line.charAt(0) == '#') {
                    continue;
                }
                String[] tokens = StringUtils.tokenizeToStringArray(line, " \t\n\r\f");
                MediaType mediaType = MediaType.parseMediaType(tokens[0]);
                for (int i = 1; i < tokens.length; i++) {
                    String fileExtension = tokens[i].toLowerCase(Locale.ENGLISH);
                    if (ext2MimeType.containsKey(fileExtension)) {
                        logger.debug("{} 已存在，旧值 '{}'，替换为新值 '{}'", fileExtension, ext2MimeType.get(fileExtension), mediaType.toString());
                    }
                    ext2MimeType.put(fileExtension, mediaType.toString());
                }
            }

            return ext2MimeType;
        } catch (IOException ex) {
            throw new IllegalStateException("Could not load '" + MIME_TYPES_FILE_NAME + "'", ex);
        }
    }

}
