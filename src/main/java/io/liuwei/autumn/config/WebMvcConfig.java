package io.liuwei.autumn.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.view.freemarker.FreeMarkerViewResolver;
import io.liuwei.autumn.interceptor.ContextInterceptor;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Autowired
    private ContextInterceptor contextInterceptor;

    @Autowired
    public void setViewResolver(FreeMarkerViewResolver viewResolver) {
        // 避免 HTTPS 跳转成 HTTP
        // 默认 true，响应 302，Location 是完整路径。如果 nginx 是 https，tomcat 是 http，那么 Location 是这样：http://xxx:443/xxx，nginx 响应 400。
        // 设为 false，响应 303，Location 是相对路径 /xxx。浏览器会正确跳转。
        viewResolver.setRedirectHttp10Compatible(false);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(contextInterceptor);
    }

}