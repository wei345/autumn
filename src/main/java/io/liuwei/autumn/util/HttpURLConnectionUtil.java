package io.liuwei.autumn.util;

import lombok.Data;
import lombok.Getter;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.lang.NonNull;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.UndeclaredThrowableException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @author liuwei
 * @since 2021-03-15 15:32
 */
@Slf4j
public class HttpURLConnectionUtil {
    private static final String CONTENT_TYPE = "Content-Type";

    static final Pattern CONTENT_TYPE_CHARSET_PARAM_PATTERN = Pattern
            .compile("(?:^|;| )charset=\"?(.+?)\"?(?: |;|$)", Pattern.CASE_INSENSITIVE);

    static final Pattern CONTENT_TYPE_IS_TEXT_PATTERN = Pattern
            .compile("\\b(text|xml|json|html|xhtml|htm)\\b", Pattern.CASE_INSENSITIVE);

    static final Pattern CONTENT_TYPE_IS_HTML_PATTERN = Pattern
            .compile("\\b(xhtml|html|htm)\\b", Pattern.CASE_INSENSITIVE);

    private static final int CONNECT_TIMEOUT_IN_MILLS = 60_000;

    private static final int READ_TIMEOUT_IN_MILLS = 300_000;

    private static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;

    public static String getForText(String url) {
        return request("GET", url, null, null)
                .getBodyAsString();
    }

    public static String postFormForText(String url, String body) {
        Map<String, List<String>> headers = new HashMap<>();
        headers.put(CONTENT_TYPE, Collections.singletonList("application/x-www-form-urlencoded"));

        return request("POST", url, headers, body.getBytes(DEFAULT_CHARSET))
                .getBodyAsString();
    }

    public static Response request(@NonNull String method, @NonNull String url,
                                   Map<String, List<String>> headers,
                                   byte[] body) {
        if (log.isInfoEnabled()) {
            log.info("==>\n{}", toHttpText(method, url, headers, body));
        }

        return connect(url, conn -> {
            conn.setRequestMethod(method);
            if (!CollectionUtils.isEmpty(headers)) {
                for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
                    String key = entry.getKey();
                    for (String value : entry.getValue()) {
                        conn.setRequestProperty(key, value);
                    }
                }
            }
            if (ArrayUtils.isNotEmpty(body)) {
                conn.setDoOutput(true);
                OutputStream out = conn.getOutputStream();
                out.write(body);
                out.flush();
                out.close();
            } else {
                conn.setDoOutput(false);
            }
            return toResponse(conn);
        });
    }

    private static StringBuilder toHttpText(@NonNull String method, @NonNull String url,
                                            Map<String, List<String>> headers, byte[] body) {
        StringBuilder message = new StringBuilder(128);
        message.append(method).append(" ").append(url).append("\n");
        if (!CollectionUtils.isEmpty(headers)) {
            message
                    .append(headers
                            .entrySet()
                            .stream()
                            .map(o -> o.getValue()
                                    .stream()
                                    .map(v -> o.getKey() + ": " + v)
                                    .collect(Collectors.joining("\n")))
                            .collect(Collectors.joining("\n")))
                    .append("\n");
        }
        if (ArrayUtils.isNotEmpty(body) && isTextContentType(headers)) {
            Charset charset = DEFAULT_CHARSET;
            String charsetName = extractCharset(headers);
            if (StringUtils.isNotBlank(charsetName)) {
                charset = Charset.forName(charsetName);
            }
            message.append("\n")
                    .append(new String(body, charset))
                    .append("\n");
        }
        return message;
    }

    private static <T> T connect(String url, RequestAction<T> action) {
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) new URL(url).openConnection();
            // 一定要 setReadTimeout，否则可能无限等待。
            conn.setConnectTimeout(CONNECT_TIMEOUT_IN_MILLS);
            conn.setReadTimeout(READ_TIMEOUT_IN_MILLS);
            return action.apply(conn);
        } catch (IOException e) {
            throw new UndeclaredThrowableException(e);
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    private static Response toResponse(HttpURLConnection conn) throws IOException {
        int status = conn.getResponseCode();
        InputStream in;
        // 如果响应状态码非 200，getInputStream() 会抛出异常，应使用 getErrorStream() 获得响应内容。
        if (isOk(status)) {
            in = conn.getInputStream();
        } else {
            in = conn.getErrorStream();
        }
        byte[] body = IOUtils.toByteArray(in);
        Map<String, List<String>> headers = conn.getHeaderFields()
                .entrySet()
                .stream()
                // 去掉状态行
                .filter(o -> o.getKey() != null)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        return new Response(status, headers, body);
    }

    private static boolean isOk(int status) {
        return status >= 200 && status < 300;
    }

    private static String extractCharset(Map<String, List<String>> headers) {
        if (headers != null && headers.size() > 0) {
            for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
                String key = entry.getKey();
                if (CONTENT_TYPE.equalsIgnoreCase(key)) {
                    for (String value : entry.getValue()) {
                        if (value != null) {
                            Matcher matcher = CONTENT_TYPE_CHARSET_PARAM_PATTERN.matcher(value);
                            if (matcher.find()) {
                                return matcher.group(1);
                            }
                        }
                    }
                }
            }
        }
        return null;
    }

    private static boolean isTextContentType(Map<String, List<String>> headers) {
        if (headers != null && headers.size() > 0) {
            for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
                String key = entry.getKey();
                if (CONTENT_TYPE.equalsIgnoreCase(key)) {
                    for (String value : entry.getValue()) {
                        if (value != null) {
                            if (CONTENT_TYPE_IS_TEXT_PATTERN.matcher(value).find()) {
                                return true;
                            }
                        }
                    }
                }
            }
        }
        return false;
    }

    private static boolean isHtmlContentType(Map<String, List<String>> headers) {
        if (headers != null && headers.size() > 0) {
            for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
                String key = entry.getKey();
                if (CONTENT_TYPE.equalsIgnoreCase(key)) {
                    for (String value : entry.getValue()) {
                        if (value != null) {
                            if (CONTENT_TYPE_IS_HTML_PATTERN.matcher(value).find()) {
                                return true;
                            }
                        }
                    }
                }
            }
        }
        return false;
    }

    private interface RequestAction<T> {
        T apply(HttpURLConnection conn) throws IOException;
    }

    @Data
    @Accessors(chain = true)
    public static class Response {
        private final int status;
        private Map<String, List<String>> headers;
        private byte[] body;
        private String charset;
        private boolean isText;
        private boolean isHtml;

        public Response(int status, Map<String, List<String>> headers, byte[] body) {
            this.status = status;
            this.headers = headers;
            this.body = body;
            this.charset = extractCharset(headers);
            this.isText = HttpURLConnectionUtil.isTextContentType(headers);
            this.isHtml = HttpURLConnectionUtil.isHtmlContentType(headers);
        }

        public String getBodyAsString() {
            String charset = this.charset == null ? DEFAULT_CHARSET.name() : this.charset;
            try {
                return new String(body, charset);
            } catch (UnsupportedEncodingException e) {
                throw new UndeclaredThrowableException(e);
            }
        }

        public boolean isOk() {
            return HttpURLConnectionUtil.isOk(status);
        }
    }

    @Getter
    public static class ErrorResponseException extends RuntimeException {
        private final int status;
        private final String responseText;

        ErrorResponseException(int status, String responseText) {
            super("status: " + status + ", responseText: " + responseText);
            this.status = status;
            this.responseText = responseText;
        }
    }
}
