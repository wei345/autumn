package xyz.liuw.autumn.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.web.ResourceProperties;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.handler.SimpleUrlHandlerMapping;
import org.springframework.web.servlet.view.freemarker.FreeMarkerViewResolver;
import xyz.liuw.autumn.interceptor.UserInterceptor;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Autowired
    private UserInterceptor userInterceptor;

    @Autowired
    public void setViewResolver(FreeMarkerViewResolver viewResolver) {
        // 避免 HTTPS 跳转成 HTTP
        // 默认 true，响应 302，Location 是完整路径。如果 nginx 是 https，tomcat 是 http，那么 Location 是这样：http://xxx:443/xxx，nginx 响应 400。
        // 设为 false，响应 303，Location 是相对路径 /xxx。浏览器会正确跳转。
        viewResolver.setRedirectHttp10Compatible(false);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(userInterceptor).excludePathPatterns("/error");
    }

    @Configuration
    public static class ResourceHttpRequestConfiguration extends WebMvcAutoConfiguration.WebMvcAutoConfigurationAdapter.FaviconConfiguration {

        public ResourceHttpRequestConfiguration(ResourceProperties resourceProperties) {
            super(resourceProperties);
        }

        @Override
        public SimpleUrlHandlerMapping faviconHandlerMapping() {
            SimpleUrlHandlerMapping mapping = super.faviconHandlerMapping();
            mapping.getUrlMap().clear();
            return mapping;
        }
    }


}