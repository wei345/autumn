package io.liuwei.autumn.aop;

import io.liuwei.autumn.annotation.AccessLevel;
import io.liuwei.autumn.model.User;
import io.liuwei.autumn.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

@Component
public class AccessLevelMethodArgumentResolver implements HandlerMethodArgumentResolver {
    @Autowired
    private UserService userService;

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(AccessLevel.class);
    }

    @Override
    public Object resolveArgument(MethodParameter parameter,
                                  ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest,
                                  WebDataBinderFactory binderFactory) throws Exception {
        ServletWebRequest swr = (ServletWebRequest) webRequest;
        User user = userService.getCurrentUser(swr.getRequest(), swr.getResponse());
        return userService.getAccessLevel(user);
    }
}