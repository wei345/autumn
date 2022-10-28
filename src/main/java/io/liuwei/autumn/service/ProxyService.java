package io.liuwei.autumn.service;

import io.liuwei.autumn.util.HttpURLConnectionUtil;
import org.apache.commons.io.IOUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.lang.reflect.UndeclaredThrowableException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;

/**
 * @author liuwei
 * @since 2022-10-27 13:45
 */
@Service
public class ProxyService {
    private static final String HOST_HEADER_NAME = "host";
    private static final String COOKIE_HEADER_NAME = "cookie";
    private static final String REFERER_HEADER_NAME = "referer";

    public ResponseEntity<?> proxy(HttpServletRequest request, String targetUrl) throws IOException {
        String method = request.getMethod();

        URI targetUri;
        try {
            targetUri = new URI(targetUrl);
        } catch (URISyntaxException e) {
            throw new UndeclaredThrowableException(e);
        }

        Map<String, List<String>> headers = getHeaders(request, targetUri);

        byte[] body = IOUtils.toByteArray(request.getInputStream());

        // request
        HttpURLConnectionUtil.Response response = HttpURLConnectionUtil.request(method, targetUrl, headers, body);

        // output
        HttpHeaders resHeaders = new HttpHeaders();
        for (Map.Entry<String, List<String>> entry : response.getHeaders().entrySet()) {
            resHeaders.addAll(entry.getKey(), entry.getValue());
        }
        return ResponseEntity.status(response.getStatus())
                .headers(resHeaders)
                .body(response.getBody());
    }

    private Map<String, List<String>> getHeaders(HttpServletRequest request, URI targetUri) {
        Map<String, List<String>> headers = new HashMap<>();
        Enumeration<String> nameEnum = request.getHeaderNames();
        while (nameEnum.hasMoreElements()) {
            String name = nameEnum.nextElement();
            if (HOST_HEADER_NAME.equalsIgnoreCase(name)
                    || COOKIE_HEADER_NAME.equalsIgnoreCase(name)
                    || REFERER_HEADER_NAME.equals(name)) {
                continue;
            }
            Enumeration<String> valueEnum = request.getHeaders(name);
            while (valueEnum.hasMoreElements()) {
                String value = valueEnum.nextElement();
                headers.computeIfAbsent(name, k -> new ArrayList<>(1))
                        .add(value);
            }
        }
        headers.put(HOST_HEADER_NAME, Collections.singletonList(targetUri.getHost()));
        return headers;
    }
}
