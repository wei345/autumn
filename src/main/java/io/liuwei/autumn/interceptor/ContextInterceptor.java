package io.liuwei.autumn.interceptor;

import io.liuwei.autumn.service.TemplateService;
import io.liuwei.autumn.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import io.liuwei.autumn.util.WebUtil;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component
public class ContextInterceptor implements HandlerInterceptor {

    private static final String ALREADY_HANDLED_ATTRIBUTE_NAME = ContextInterceptor.class.getName() + ".handled";

    @Autowired
    private UserService userService;

    @Autowired
    private TemplateService templateService;

    // 同 org.springframework.boot.autoconfigure.web.servlet.error.BasicErrorController
    @Value("${server.error.path:${error.path:/error}}")
    private String errorPath;

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response, Object handler) {
        if (request.getAttribute(ALREADY_HANDLED_ATTRIBUTE_NAME) == null) {
            request.setAttribute(ALREADY_HANDLED_ATTRIBUTE_NAME, Boolean.TRUE);
            userService.setCurrentUser(request, response);
            templateService.setCtx(request);

            // 直接访问错误页面响应 404
            if (errorPath.equals(WebUtil.getInternalPath(request))
                    && request.getAttribute("javax.servlet.error.status_code") == null) {
                try {
                    response.sendError(HttpStatus.NOT_FOUND.value(), HttpStatus.NOT_FOUND.getReasonPhrase());
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        return true;
    }
}