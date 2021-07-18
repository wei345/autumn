package io.liuwei.autumn.util;

import lombok.Getter;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * @author liuwei
 * @since 2021-03-15 15:32
 */
public class HttpURLConnectionUtil {
    // 一定要 setReadTimeout，否则可能无限等待。
    // 如果响应状态码非 200，getInputStream() 会抛出异常，应使用 getErrorStream() 获得响应内容。

    public static String get(String url) {
        return connect(url, conn -> {
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            conn.setReadTimeout(300000);
            conn.setDoOutput(false);
            return handleResponse(conn);
        });
    }

    public static String post(String url, String body) {
        return connect(url, conn -> {
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            conn.setReadTimeout(300000);
            conn.setDoOutput(true);
            OutputStream out = conn.getOutputStream();

            out.write(body.getBytes());
            out.flush();
            out.close();
            return handleResponse(conn);
        });
    }

    private static <T> T connect(String url, RequestAction<T> action) {
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) new URL(url).openConnection();
            return action.apply(conn);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    private static String handleResponse(HttpURLConnection conn) throws IOException {
        int status = conn.getResponseCode();
        boolean isOk = status >= 200 && status < 300;

        InputStream in;
        if (isOk) {
            in = conn.getInputStream();
        } else {
            in = conn.getErrorStream();
        }
        String responseText = IOUtils.toString(in, "UTF-8");

        if (isOk) {
            return responseText;
        } else {
            throw new ErrorResponseException(status, responseText);
        }
    }

    private interface RequestAction<T> {
        T apply(HttpURLConnection conn) throws IOException;
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
