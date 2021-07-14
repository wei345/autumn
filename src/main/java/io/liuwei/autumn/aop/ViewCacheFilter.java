package io.liuwei.autumn.aop;

import io.liuwei.autumn.component.MediaRevisionResolver;
import io.liuwei.autumn.model.RevisionContent;
import io.liuwei.autumn.util.MediaTypeUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.Cache;
import org.springframework.cache.interceptor.SimpleKey;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;
import org.springframework.web.util.WebUtils;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;

/**
 * 缓存视图渲染结果，之后的请求如果 key 相等，直接返回缓存里的数据，不重复渲染，提高性能。
 *
 * @author liuwei602099
 * @since 2021-07-14 14:55
 */
@Component
public class ViewCacheFilter extends OncePerRequestFilter {
    private static final String CACHE_KEY_ATTRIBUTE = ViewCacheFilter.class.getName() + ".KEY";

    @Autowired
    @Qualifier("viewCache")
    private Cache viewCache;

    @Autowired
    private MediaRevisionResolver mediaRevisionResolver;

    public static void enableContentCaching(ServletRequest request, SimpleKey cacheKey) {
        request.setAttribute(CACHE_KEY_ATTRIBUTE, cacheKey);
    }

    private static boolean isContentCachingEnabled(ServletRequest request) {
        return request.getAttribute(CACHE_KEY_ATTRIBUTE) != null;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        HttpServletResponse responseToUse = response;
        if (!isAsyncDispatch(request) && !(response instanceof ContentCachingResponseWrapper)) {
            responseToUse = new ViewContentCachingResponseWrapper(response, request);
        }

        filterChain.doFilter(request, responseToUse);

        if (!isAsyncStarted(request) && isContentCachingEnabled(request)) {
            updateResponse(request, responseToUse);
        }
    }

    private void updateResponse(HttpServletRequest request, HttpServletResponse response) throws IOException {
        ContentCachingResponseWrapper responseWrapper =
                WebUtils.getNativeResponse(response, ContentCachingResponseWrapper.class);
        Assert.notNull(responseWrapper, "ContentCachingResponseWrapper not found");
        HttpServletResponse rawResponse = (HttpServletResponse) responseWrapper.getResponse();
        int statusCode = responseWrapper.getStatusCode();

        if (isEligibleForCache(request, responseWrapper, statusCode)) {
            RevisionContent rc = setCache(request, responseWrapper);
            if (!rawResponse.isCommitted()
                    && new ServletWebRequest(request, response).checkNotModified(rc.getEtag())) {
                rawResponse.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
            } else {
                responseWrapper.copyBodyToResponse();
            }
        } else {
            responseWrapper.copyBodyToResponse();
        }
    }

    protected boolean isEligibleForCache(HttpServletRequest request, HttpServletResponse response,
                                         int responseStatusCode) {
        String method = request.getMethod();
        return responseStatusCode >= 200 && responseStatusCode < 300 && HttpMethod.GET.matches(method);
    }

    private RevisionContent setCache(HttpServletRequest request, ContentCachingResponseWrapper response) {
        SimpleKey cacheKey = (SimpleKey) request.getAttribute(CACHE_KEY_ATTRIBUTE);
        return viewCache.get(cacheKey, () -> {
            byte[] bytes = response.getContentAsByteArray();
            return mediaRevisionResolver.toRevisionContent(bytes, MediaTypeUtil.TEXT_HTML_UTF8);
        });
    }

    @SuppressWarnings("WeakerAccess")
    private static class ViewContentCachingResponseWrapper extends ContentCachingResponseWrapper {

        private final HttpServletRequest request;
        private final HttpServletResponse response;

        public ViewContentCachingResponseWrapper(HttpServletResponse response, HttpServletRequest request) {
            super(response);
            this.request = request;
            this.response = response;
        }

        @Override
        public void setStatus(int sc) {
            if (useWrapperResponse()) {
                super.setStatus(sc);
            } else {
                response.setStatus(sc);
            }
        }

        @Override
        public void setStatus(int sc, String sm) {
            if (useWrapperResponse()) {
                super.setStatus(sc, sm);
            } else {
                response.setStatus(sc, sm);
            }
        }

        @Override
        public void sendError(int sc) throws IOException {
            if (useWrapperResponse()) {
                super.sendError(sc);
            } else {
                response.sendError(sc);
            }
        }

        @Override
        public void sendError(int sc, String msg) throws IOException {
            if (useWrapperResponse()) {
                super.sendError(sc, msg);
            } else {
                response.sendError(sc, msg);
            }
        }

        @Override
        public void sendRedirect(String location) throws IOException {
            if (useWrapperResponse()) {
                super.sendRedirect(location);
            } else {
                response.sendRedirect(location);
            }
        }

        @Override
        public ServletOutputStream getOutputStream() throws IOException {
            if (useWrapperResponse()) {
                return super.getOutputStream();
            } else {
                return response.getOutputStream();
            }
        }

        @Override
        public PrintWriter getWriter() throws IOException {
            if (useWrapperResponse()) {
                return super.getWriter();
            } else {
                return response.getWriter();
            }
        }

        @Override
        public void flushBuffer() throws IOException {
            if (useWrapperResponse()) {
                super.flushBuffer();
            } else {
                response.flushBuffer();
            }
        }

        @Override
        public void setContentLength(int len) {
            if (useWrapperResponse()) {
                super.setContentLength(len);
            } else {
                response.setContentLength(len);
            }
        }

        @Override
        public void setContentLengthLong(long len) {
            if (useWrapperResponse()) {
                super.setContentLengthLong(len);
            } else {
                response.setContentLengthLong(len);
            }
        }

        @Override
        public void setBufferSize(int size) {
            if (useWrapperResponse()) {
                super.setBufferSize(size);
            } else {
                response.setBufferSize(size);
            }
        }

        @Override
        public void resetBuffer() {
            if (useWrapperResponse()) {
                super.resetBuffer();
            } else {
                response.resetBuffer();
            }
        }

        @Override
        public void reset() {
            if (useWrapperResponse()) {
                super.reset();
            } else {
                response.reset();
            }
        }

        @Override
        public int getStatusCode() {
            if (useWrapperResponse()) {
                return super.getStatusCode();
            } else {
                return response.getStatus();
            }
        }

        @Override
        public byte[] getContentAsByteArray() {
            if (useWrapperResponse()) {
                return super.getContentAsByteArray();
            } else {
                throw new IllegalStateException();
            }
        }

        @Override
        public InputStream getContentInputStream() {
            if (useWrapperResponse()) {
                return super.getContentInputStream();
            } else {
                throw new IllegalStateException();
            }
        }

        @Override
        public int getContentSize() {
            if (useWrapperResponse()) {
                return super.getContentSize();
            } else {
                throw new IllegalStateException();
            }
        }

        @Override
        public void copyBodyToResponse() throws IOException {
            if (useWrapperResponse()) {
                super.copyBodyToResponse();
            } else {
                throw new IllegalStateException();
            }
        }

        @Override
        protected void copyBodyToResponse(boolean complete) throws IOException {
            if (useWrapperResponse()) {
                super.copyBodyToResponse(complete);
            } else {
                throw new IllegalStateException();
            }
        }

        private boolean useWrapperResponse() {
            return isContentCachingEnabled(this.request);
        }
    }
}
