package io.liuwei.autumn.aop;

import org.springframework.stereotype.Component;
import org.springframework.web.filter.ShallowEtagHeaderFilter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.InputStream;

/**
 * @author liuwei
 * @since 2021-07-12 00:03
 */
@Component
public class EtagHeaderFilter extends ShallowEtagHeaderFilter {
    private static final String HEADER_ETAG = "ETag";

    @Override
    protected boolean isEligibleForEtag(HttpServletRequest request, HttpServletResponse response,
                                        int responseStatusCode, InputStream inputStream) {
        return super.isEligibleForEtag(request, response, responseStatusCode, inputStream)
                && !response.containsHeader(HEADER_ETAG);
    }
}
