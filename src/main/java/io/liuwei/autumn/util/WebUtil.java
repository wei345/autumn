package io.liuwei.autumn.util;

import com.vip.vjtools.vjkit.text.EscapeUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.StringTokenizer;

/**
 * @author liuwei
 * Created by liuwei on 2018/11/22.
 */
public class WebUtil {

    private static final long DEFAULT_EXPIRES_SECONDS = 86400 * 365 * 16; // 16 年

    public static String getClientIpAddress(HttpServletRequest request) {
        String clientIp;
        String xForwardedForHeader = request.getHeader("X-Forwarded-For");
        if (xForwardedForHeader == null) {
            clientIp = request.getRemoteAddr();
        } else {
            // As of https://en.wikipedia.org/wiki/X-Forwarded-For
            // The general format of the field is: X-Forwarded-For: client, proxy1, proxy2 ...
            // we only want the client
            clientIp = new StringTokenizer(xForwardedForHeader, ",").nextToken().trim();
        }
        if ("0:0:0:0:0:0:0:1".equals(clientIp)) {
            clientIp = "127.0.0.1";
        }
        return clientIp;
    }

    public static String getInternalPath(HttpServletRequest request) {
        return EscapeUtil.urlDecode(request.getRequestURI())
                .substring(request.getContextPath().length());
    }

    public static boolean checkNotModified(WebRequest webRequest, String etag, String requestRevisionParamName) {
        if (webRequest instanceof ServletWebRequest) {
            HttpServletRequest request = ((ServletRequestAttributes) webRequest).getRequest();
            HttpServletResponse response = ((ServletRequestAttributes) webRequest).getResponse();
            setCacheTimeIfVersionMatch(request, response, requestRevisionParamName, etag, DEFAULT_EXPIRES_SECONDS);
        }

        return webRequest.checkNotModified(etag);
    }

    @SuppressWarnings("SameParameterValue")
    private static void setCacheTimeIfVersionMatch(
            HttpServletRequest request, HttpServletResponse response, String requestRevisionParamName,
            String etag, long expiresSeconds) {

        String requestVersion = request.getParameter(requestRevisionParamName);
        if (requestVersion == null || !etag.startsWith(requestVersion, 1)) {
            return;
        }

        // Http 1.0 header, set a fix expires date.
        response.setDateHeader(HttpHeaders.EXPIRES, System.currentTimeMillis() + (expiresSeconds * 1000));
        // Http 1.1 header, set a time after now.
        response.setHeader(HttpHeaders.CACHE_CONTROL, "private, max-age=" + expiresSeconds);
    }

    public static boolean isSecure(HttpServletRequest request) {
        return "https".equalsIgnoreCase(getScheme(request));
    }

    private static String getScheme(HttpServletRequest request) {
        String scheme = request.getHeader("X-Forwarded-Proto");
        if (StringUtils.isNotBlank(scheme)) {
            return scheme;
        }
        return request.getScheme();
    }

}
