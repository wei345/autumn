package io.liuwei.autumn.util;

import com.vip.vjtools.vjkit.text.EscapeUtil;
import io.liuwei.autumn.constant.Constants;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.lang.Nullable;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.util.CookieGenerator;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Enumeration;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    // ---- ETag ----

    /**
     * 检查 request Header ETag 跟指定的 <code>etag</code> 是否匹配。
     * <p>
     * 还会检查 request 的 {@value Constants#REQUEST_PARAMETER_REVISION} 参数，
     * 如果跟指定的 <code>etag</code> 匹配，会设置 response Header {@value HttpHeaders#EXPIRES} 和
     * {@value HttpHeaders#CACHE_CONTROL}，使浏览器在缓存过期前不发送请求。
     */
    public static boolean checkNotModified(String revision, String etag,
                                           HttpServletRequest request, HttpServletResponse response) {
        String reqRevision = request.getParameter(Constants.REQUEST_PARAMETER_REVISION);
        if (revision.equals(reqRevision)) {
            // Http 1.0 header, set a fix expires date.
            response.setDateHeader(HttpHeaders.EXPIRES, System.currentTimeMillis() + (DEFAULT_EXPIRES_SECONDS * 1000));
            // Http 1.1 header, set a time after now.
            response.setHeader(HttpHeaders.CACHE_CONTROL, "private, max-age=" + DEFAULT_EXPIRES_SECONDS);
        }
        return checkNotModified(etag, request);
    }

    /**
     * Pattern matching ETag multiple field values in headers such as "If-Match", "If-None-Match".
     *
     * @see <a href="https://tools.ietf.org/html/rfc7232#section-2.3">Section 2.3 of RFC 7232</a>
     */
    private static final Pattern ETAG_HEADER_VALUE_PATTERN = Pattern.compile("\\*|\\s*((W\\/)?(\"[^\"]*\"))\\s*,?");

    /**
     * 不同于 {@link ServletWebRequest#checkNotModified(String)}，
     * 此方法只检查 ETag 是否匹配，不设置 ETag，不设置 304。
     */
    public static boolean checkNotModified(@Nullable String etag, HttpServletRequest request) {
        if (!org.springframework.util.StringUtils.hasLength(etag)) {
            return false;
        }

        Enumeration<String> ifNoneMatch;
        try {
            ifNoneMatch = request.getHeaders(HttpHeaders.IF_NONE_MATCH);
        } catch (IllegalArgumentException ex) {
            return false;
        }
        if (!ifNoneMatch.hasMoreElements()) {
            return false;
        }

        // We will perform this validation...
        etag = padEtagIfNecessary(etag);
        if (etag.startsWith("W/")) {
            etag = etag.substring(2);
        }
        while (ifNoneMatch.hasMoreElements()) {
            String clientETags = ifNoneMatch.nextElement();
            Matcher etagMatcher = ETAG_HEADER_VALUE_PATTERN.matcher(clientETags);
            // Compare weak/strong ETags as per https://tools.ietf.org/html/rfc7232#section-2.3
            while (etagMatcher.find()) {
                if (org.springframework.util.StringUtils.hasLength(etagMatcher.group()) && etag.equals(etagMatcher.group(3))) {
                    return true;
                }
            }
        }

        return false;
    }

    private static String padEtagIfNecessary(String etag) {
        if (!org.springframework.util.StringUtils.hasLength(etag)) {
            return etag;
        }
        if ((etag.startsWith("\"") || etag.startsWith("W/\"")) && etag.endsWith("\"")) {
            return etag;
        }
        return "\"" + etag + "\"";
    }

    public static void setEtag(String etag, HttpServletResponse response) {
        response.setHeader(HttpHeaders.ETAG, padEtagIfNecessary(etag));
    }

    // ---- Cookie ----

    @SuppressWarnings("SameParameterValue")
    public static Cookie getCookie(String name, HttpServletRequest request) {
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if (cookie.getName().equals(name)) {
                    return cookie;
                }
            }
        }
        return null;
    }

    public static void deleteCookie(String name, HttpServletRequest request, HttpServletResponse response) {
        CookieGenerator cg = new CookieGenerator();
        cg.setCookieName(name);
        cg.setCookieMaxAge(0);
        cg.setCookieHttpOnly(true);
        addCookie(cg, null, request, response);
    }

    public static void addCookie(CookieGenerator cg, String value, HttpServletRequest request, HttpServletResponse response) {
        cg.setCookiePath(request.getContextPath() + "/");
        cg.setCookieSecure(WebUtil.isSecure(request));
        cg.addCookie(response, value);
    }

    private static boolean isSecure(HttpServletRequest request) {
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
