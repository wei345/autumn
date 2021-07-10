package io.liuwei.autumn.interceptor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

//@Component
@Order(1)
public class ExceptionHandlerFilter implements Filter {

    private static Logger logger = LoggerFactory.getLogger(ExceptionHandlerFilter.class);

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) {
        try {
            chain.doFilter(request, response);
        } catch (Exception e) {
            logger.warn("", e);
            try {
                ((HttpServletResponse) response).sendError(
                        HttpStatus.INTERNAL_SERVER_ERROR.value(),
                        HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase());
            } catch (IOException e1) {
                logger.warn("Failed to call response#sendError", e);
            }
        }
    }
}