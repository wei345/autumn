package io.liuwei.autumn.aop;

import io.liuwei.autumn.manager.RevisionContentManager;
import io.liuwei.autumn.model.RevisionContent;
import io.liuwei.autumn.util.MediaTypeUtil;
import io.liuwei.autumn.util.WebUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.Cache;
import org.springframework.cache.interceptor.SimpleKey;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * 缓存视图渲染结果。
 *
 * @author liuwei602099
 * @since 2021-07-14 14:55
 */
@SuppressWarnings({"WeakerAccess", "NullableProblems"})
@Component
public class ViewCacheFilter extends OncePerRequestFilter {
    private static final String CACHE_KEY_ATTRIBUTE = ViewCacheFilter.class.getName() + ".KEY";
    private final AntPathMatcher antPathMatcher = new AntPathMatcher();
    @Autowired
    @Qualifier("viewCache")
    private Cache viewCache;
    @Autowired
    private RevisionContentManager revisionContentManager;

    public static void enableContentCaching(ServletRequest request, SimpleKey cacheKey) {
        request.setAttribute(CACHE_KEY_ATTRIBUTE, cacheKey);
    }

    private static boolean isContentCachingEnabled(ServletRequest request) {
        return request.getAttribute(CACHE_KEY_ATTRIBUTE) != null;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String path = WebUtil.getInternalPath(request);

        if (!HttpMethod.GET.matches(request.getMethod())
                || isAsyncDispatch(request)
                || antPathMatcher.match("/**/*.*", path)) {

            filterChain.doFilter(request, response);

        } else {

            HttpServletResponse responseToUse = response;
            if (!(response instanceof ContentCachingResponseWrapper)) {
                responseToUse = new ViewContentCachingResponseWrapper(response, request);
            }

            filterChain.doFilter(request, responseToUse);

            if (isContentCachingEnabled(request)) {
                handleContentCache(request, responseToUse);
            }
        }
    }

    private void handleContentCache(HttpServletRequest request, HttpServletResponse response) throws IOException {
        ContentCachingResponseWrapper responseWrapper = (ContentCachingResponseWrapper) response;

        int statusCode = responseWrapper.getStatus();
        if (statusCode >= 200 && statusCode < 300) {
            RevisionContent rc = setCache(request, responseWrapper);

            HttpServletResponse rawResponse = (HttpServletResponse) responseWrapper.getResponse();
            if (!rawResponse.isCommitted() && WebUtil.checkNotModified(rc.getEtag(), request)) {
                rawResponse.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
                WebUtil.setEtag(rc.getEtag(), rawResponse);
            } else {
                WebUtil.setEtag(rc.getEtag(), responseWrapper);
                responseWrapper.copyBodyToResponse();
            }

        } else {
            responseWrapper.copyBodyToResponse();
        }
    }

    private RevisionContent setCache(HttpServletRequest request, ContentCachingResponseWrapper response) {
        return viewCache.get(request.getAttribute(CACHE_KEY_ATTRIBUTE), () -> {
            byte[] content = response.getContentAsByteArray();
            return revisionContentManager.toRevisionContent(content, MediaTypeUtil.TEXT_HTML_UTF8);
        });
    }

    private static class ViewContentCachingResponseWrapper extends ContentCachingResponseWrapper {

        private final HttpServletRequest request;
        private final HttpServletResponse response;

        public ViewContentCachingResponseWrapper(HttpServletResponse response, HttpServletRequest request) {
            super(response);
            this.request = request;
            this.response = response;
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

        private boolean useWrapperResponse() {
            return isContentCachingEnabled(this.request);
        }
    }
}
