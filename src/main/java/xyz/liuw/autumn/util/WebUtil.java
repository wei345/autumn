package xyz.liuw.autumn.util;

import com.vip.vjtools.vjkit.text.EscapeUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import java.util.StringTokenizer;

/**
 * @author liuwei
 * Created by liuwei on 2018/11/22.
 */
@Component
public class WebUtil {

    @Value("${server.servlet.context-path:}")
    private String contextPath;

    @Value("${autumn.etag.version}")
    private int etagVersion;

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

    public static String getRelativePath(HttpServletRequest request) {
        return EscapeUtil.urlDecode(request.getRequestURI())
                .substring(request.getContextPath().length());
    }

    public String getEtag(String md5) {
        return getEtag(etagVersion, md5);
    }

    public static String getEtag(int etagVersion, String md5) {
        return "\"" + etagVersion + "|" + md5 + "\"";
    }

    public String getContextPath() {
        return contextPath;
    }

}
