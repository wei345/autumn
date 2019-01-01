package xyz.liuw.autumn.util;

import com.vip.vjtools.vjkit.text.EscapeUtil;
import org.springframework.beans.factory.annotation.Value;
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
@Component
public class WebUtil {

    private static final String ETAG_MD5_SEPARATOR = ".";
    // 修改 etag version 会导致所有客户端页面缓存失效。某些情况下你可能想修改这个值，例如修改了 response CharacterEncoding
    private static final int ETAG_VERSION = 1;
    private static final String REQUEST_VERSION_KEY = "v";
    private static final String VERSION_KEY_VALUE_PREFIX = REQUEST_VERSION_KEY + "=" + ETAG_VERSION + ETAG_MD5_SEPARATOR;
    private static final long DEFAULT_EXPIRES_SECONDS = 86400;
    @Value("${server.servlet.context-path:}")
    private String contextPath;

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

    public static String getEtag(String md5) {
        return "\"" + ETAG_VERSION + ETAG_MD5_SEPARATOR + md5 + "\"";
    }

    /**
     * @return e.g. v=1.1234567
     */
    public static String getVersionKeyValue(String md5) {
        return VERSION_KEY_VALUE_PREFIX + md5.substring(0, 7);
    }

    public static boolean checkNotModified(WebRequest webRequest, String etag) {
        if (webRequest instanceof ServletWebRequest) {
            HttpServletRequest request = ((ServletRequestAttributes) webRequest).getRequest();
            HttpServletResponse response = ((ServletRequestAttributes) webRequest).getResponse();
            setExpiresHeaderIfVersionMatch(request, response, etag, DEFAULT_EXPIRES_SECONDS);
        }

        return webRequest.checkNotModified(etag);
    }

    @SuppressWarnings("SameParameterValue")
    private static void setExpiresHeaderIfVersionMatch(HttpServletRequest request, HttpServletResponse response, String etag, long expiresSeconds) {

        String requestVersion = request.getParameter(REQUEST_VERSION_KEY);
        if (requestVersion == null || !etag.startsWith(requestVersion, 1)) {
            return;
        }

        // Http 1.0 header, set a fix expires date.
        response.setDateHeader(HttpHeaders.EXPIRES, System.currentTimeMillis() + (expiresSeconds * 1000));
        // Http 1.1 header, set a time after now.
        response.setHeader(HttpHeaders.CACHE_CONTROL, "private, max-age=" + expiresSeconds);
    }

    public String getContextPath() {
        return contextPath;
    }

}
