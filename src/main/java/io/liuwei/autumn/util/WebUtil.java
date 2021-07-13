package io.liuwei.autumn.util;

import com.vip.vjtools.vjkit.text.EscapeUtil;
import io.liuwei.autumn.constant.Constants;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpHeaders;
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

    /**
     * 除了设置 etag 之外，还会检查请求的 {@link Constants#REQUEST_PARAMETER_REVISION} 参数，
     * 如果跟 etag 匹配，会设置缓存时间，使用浏览器在缓存期间不再发送请求。
     */
    public static boolean checkNotModified(String revision, String etag, WebRequest webRequest) {
        if (webRequest instanceof ServletWebRequest) {
            HttpServletRequest request = ((ServletRequestAttributes) webRequest).getRequest();
            HttpServletResponse response = ((ServletRequestAttributes) webRequest).getResponse();
            String reqRevision = request.getParameter(Constants.REQUEST_PARAMETER_REVISION);
            if (revision.equals(reqRevision)) {
                // Http 1.0 header, set a fix expires date.
                //noinspection ConstantConditions
                response.setDateHeader(HttpHeaders.EXPIRES, System.currentTimeMillis() + (DEFAULT_EXPIRES_SECONDS * 1000));
                // Http 1.1 header, set a time after now.
                response.setHeader(HttpHeaders.CACHE_CONTROL, "private, max-age=" + DEFAULT_EXPIRES_SECONDS);
            }
        }
        return webRequest.checkNotModified(etag);
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
