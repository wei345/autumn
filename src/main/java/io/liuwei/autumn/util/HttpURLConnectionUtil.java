package io.liuwei.autumn.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Array;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 基于 {@link HttpURLConnection} 的 HTTP 客户端。
 * <p>
 * 支持多线程并发。
 *
 * @author liuwei
 * @since 2021-03-15 15:32
 */
@SuppressWarnings("SameParameterValue")
public class HttpURLConnectionUtil {
    private static final Logger log = LoggerFactory.getLogger(HttpURLConnectionUtil.class);

    private static final String CONTENT_TYPE = "Content-Type";

    static final Pattern CONTENT_TYPE_CHARSET_PARAM_PATTERN = Pattern
            .compile("(?:^|;| )charset=\"?(.+?)\"?(?: |;|$)", Pattern.CASE_INSENSITIVE);

    static final Pattern CONTENT_TYPE_IS_TEXT_PATTERN = Pattern
            .compile("\\b(text|xml|json|html|xhtml|htm)\\b", Pattern.CASE_INSENSITIVE);

    static final Pattern CONTENT_TYPE_IS_HTML_PATTERN = Pattern
            .compile("\\b(xhtml|html|htm)\\b", Pattern.CASE_INSENSITIVE);

    private static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;

    private static final int DEFAULT_CONNECT_TIMEOUT_IN_MILLS = 60_000;

    private static final int DEFAULT_READ_TIMEOUT_IN_MILLS = 300_000;

    public static Response request(String method, String url,
                                   Map<String, ?> headers, byte[] body) {
        return request(method, url, headers, body, DEFAULT_CONNECT_TIMEOUT_IN_MILLS,
                DEFAULT_READ_TIMEOUT_IN_MILLS);
    }

    public static Response request(String method, String url,
                                   Map<String, ?> headers, byte[] body,
                                   int connectTimeoutInMills, int readTimeoutInMills) {
        if (isBlank(method)) {
            throw new IllegalArgumentException("method");
        }
        if (isBlank(url)) {
            throw new IllegalArgumentException("url");
        }

        String httpMethod = method.toUpperCase();
        Map<String, List<String>> normalizedHeaders = normalizeHeaders(headers);
        if (log.isDebugEnabled()) {
            log.debug("==> {}", toHttpStyleText(httpMethod, url, normalizedHeaders, body));
        }

        return connect(url, connectTimeoutInMills, readTimeoutInMills, conn -> {
            conn.setRequestMethod(httpMethod);
            if (isNotEmpty(normalizedHeaders)) {
                for (Map.Entry<String, List<String>> entry : normalizedHeaders.entrySet()) {
                    String key = entry.getKey();
                    for (String value : entry.getValue()) {
                        conn.setRequestProperty(key, value);
                    }
                }
            }
            if (isNotEmpty(body)) {
                conn.setDoOutput(true);
                OutputStream out = conn.getOutputStream();
                out.write(body);
                out.flush();
                out.close();
            } else {
                conn.setDoOutput(false);
            }

            Response response = getResponse(conn);
            if (log.isDebugEnabled()) {
                log.debug("<== {}", response);
            }
            return response;
        });
    }

    private static <T> T connect(String url, int connectTimeoutInMills,
                                 int readTimeoutInMills, RequestAction<T> action) {
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) new URL(url).openConnection();
            // 一定要 setReadTimeout，否则可能无限等待。
            conn.setConnectTimeout(connectTimeoutInMills);
            conn.setReadTimeout(readTimeoutInMills);
            return action.apply(conn);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    private interface RequestAction<T> {
        T apply(HttpURLConnection conn) throws IOException;
    }

    private static Response getResponse(HttpURLConnection conn) throws IOException {
        int status = conn.getResponseCode();
        InputStream in;
        // 如果响应状态码非 200，getInputStream() 会抛出异常，应使用 getErrorStream() 获得响应内容。
        if (isOk(status)) {
            in = conn.getInputStream();
        } else {
            in = conn.getErrorStream();
        }
        byte[] body = toByteArray(in);

        Map<String, List<String>> headers = conn.getHeaderFields()
                .entrySet()
                .stream()
                .filter(o -> o.getKey() != null) // 去掉状态行
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        return new Response(status, headers, body);
    }

    public static class Response {
        private final int status;
        private final Map<String, List<String>> headers;
        private final byte[] body;

        public Response(int status, Map<String, List<String>> headers, byte[] body) {
            this.status = status;
            this.headers = headers;
            this.body = body;
        }

        public int getStatus() {
            return status;
        }

        public boolean isOk() {
            return HttpURLConnectionUtil.isOk(status);
        }

        public Map<String, List<String>> getHeaders() {
            return headers;
        }

        public boolean isText() {
            return isContentTypeText(headers);
        }

        public boolean isHtml() {
            return isContentTypeHtml(headers);
        }

        public String getCharset() {
            return extractCharset(headers);
        }

        public byte[] getBody() {
            return body;
        }

        public String getBodyAsString() {
            return new String(body, decideCharset(headers));
        }

        @Override
        public String toString() {
            return toHttpStyleText(String.valueOf(status), null,
                    headers, body);
        }
    }

    private static String toHttpStyleText(String firstLinePart1, String firstLinePart2,
                                          Map<String, List<String>> headers, byte[] body) {
        StringBuilder stringBuilder = new StringBuilder(128);
        stringBuilder.append(firstLinePart1);
        if (isNotBlank(firstLinePart2)) {
            stringBuilder.append(" ").append(firstLinePart2);
        }
        stringBuilder.append("\n");
        if (isNotEmpty(headers)) {
            headers.forEach((key, value) ->
                    each(value, s ->
                            stringBuilder.append(key).append(": ").append(s).append("\n")));
        }
        if (isNotEmpty(body)) {
            stringBuilder.append("\n");
            if (isContentTypeText(headers)) {
                stringBuilder
                        .append(new String(body, decideCharset(headers)))
                        .append("\n");
            } else {
                stringBuilder.append("<").append(body.length).append(" bytes>\n");
            }
        }
        return stringBuilder.toString();
    }

    private static Map<String, List<String>> normalizeHeaders(Map<String, ?> roughHeaders) {
        if (roughHeaders == null) {
            return null;
        }
        Map<String, List<String>> headers = new HashMap<>((int) (roughHeaders.size() / 0.75 + 1));
        roughHeaders.forEach((key, value) -> {
            List<String> values = new ArrayList<>();
            each(value, values::add);
            headers.put(key, values);
        });
        return headers;
    }

    private static void each(Object obj, Consumer<String> consumer) {
        if (obj == null) {
            return;
        }
        if (obj.getClass().isArray()) {
            for (int i = 0, len = Array.getLength(obj); i < len; i++) {
                Object element = Array.get(obj, i);
                consumer.accept(String.valueOf(element));
            }
        } else if (obj instanceof Iterable) {
            for (Object element : (Iterable<?>) obj) {
                consumer.accept(String.valueOf(element));
            }
        } else {
            consumer.accept(String.valueOf(obj));
        }
    }

    private static byte[] toByteArray(InputStream input) {
        try (final ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buff = new byte[1024];
            int len;
            while ((len = input.read(buff)) != -1) {
                if (len > 0) {
                    output.write(buff, 0, len);
                }
            }
            return output.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static boolean isOk(int status) {
        return status >= 200 && status < 300;
    }

    private static Charset decideCharset(Map<String, List<String>> headers) {
        Charset charset = DEFAULT_CHARSET;
        String charsetName = extractCharset(headers);
        if (isNotBlank(charsetName)) {
            charset = Charset.forName(charsetName);
        }
        return charset;
    }

    private static String extractCharset(Map<String, List<String>> headers) {
        Matcher matcher = findHeaderValue(headers, CONTENT_TYPE, CONTENT_TYPE_CHARSET_PARAM_PATTERN);
        return matcher == null ? null : matcher.group(1);
    }

    private static boolean isContentTypeText(Map<String, List<String>> headers) {
        return findHeaderValue(headers, CONTENT_TYPE, CONTENT_TYPE_IS_TEXT_PATTERN) != null;
    }

    private static boolean isContentTypeHtml(Map<String, List<String>> headers) {
        return findHeaderValue(headers, CONTENT_TYPE, CONTENT_TYPE_IS_HTML_PATTERN) != null;
    }

    private static Matcher findHeaderValue(Map<String, List<String>> headers,
                                           String name, Pattern valuePattern) {
        if (headers != null && headers.size() > 0) {
            for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
                String key = entry.getKey();
                if (name.equalsIgnoreCase(key)) {
                    for (String value : entry.getValue()) {
                        if (value != null) {
                            Matcher matcher = valuePattern.matcher(value);
                            if (matcher.find()) {
                                return matcher;
                            }
                        }
                    }
                }
            }
        }
        return null;
    }

    private static boolean isNotEmpty(byte[] array) {
        return array != null && array.length > 0;
    }

    private static boolean isNotEmpty(Map<?, ?> map) {
        return map != null && map.size() > 0;
    }

    private static boolean isNotBlank(String string) {
        return !isBlank(string);
    }

    private static boolean isBlank(String string) {
        return string == null || string.trim().length() == 0;
    }

}
