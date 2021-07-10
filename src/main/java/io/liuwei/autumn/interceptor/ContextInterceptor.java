package io.liuwei.autumn.interceptor;

import com.google.common.io.BaseEncoding;
import io.liuwei.autumn.ArticleService;
import io.liuwei.autumn.Constants;
import io.liuwei.autumn.MediaRevisionResolver;
import io.liuwei.autumn.enums.AccessLevelEnum;
import io.liuwei.autumn.model.RevisionContent;
import io.liuwei.autumn.model.User;
import io.liuwei.autumn.service.StaticService;
import io.liuwei.autumn.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static java.nio.charset.StandardCharsets.UTF_8;

@Component
public class ContextInterceptor implements HandlerInterceptor {

    private static final String ALREADY_HANDLED_ATTRIBUTE_NAME = ContextInterceptor.class.getName() + ".handled";

    @Autowired
    private UserService userService;

    @Autowired
    private StaticService staticService;

    @Autowired
    private ArticleService articleService;

    @Autowired
    private MediaRevisionResolver mediaRevisionResolver;

    @Value("${autumn.site-title}")
    private String siteTitle;

    @Value("${server.servlet.context-path:}")
    private String contextPath;

    /**
     * cookie key 和 localStorage key 前缀
     */
    private String prefix;

    @PostConstruct
    private void init() {
        String prefix = "autumn.";
        // 可以把域名相同 port 不同或 contentPath 不同的 Autumn 实例看作不同实例。
        // port 不是配置文件里的 server.port，是 request url 里的 port，
        // 也就是 Host header 里的 port，如果有反向代理，需要把 Host 传给 Autumn。
        // 每次计算 instance 需要从 request 中读取 port，把 request 一路传过来有点麻烦。
        // 可以简单省事直接在配置文件里加一条配置，指定最终用户访问的 port。
        // 但我不需要区分 port，所以这里只考虑 contextPath。
        // 我设置 Autumn contextPath 和最终用户（经反向代理）访问的 contextPath 是相同的。
        if (contextPath.length() > 0) {
            prefix = prefix + BaseEncoding.base64Url().omitPadding().encode(contextPath.getBytes(UTF_8)) + ".";
        }
        this.prefix = prefix;
    }

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response, Object handler) {
        if (request.getAttribute(ALREADY_HANDLED_ATTRIBUTE_NAME) == null) {
            request.setAttribute(ALREADY_HANDLED_ATTRIBUTE_NAME, Boolean.TRUE);

            User user = userService.getCurrentUser(request, response);
            AccessLevelEnum accessLevel = userService.getAccessLevel(user);
            request.setAttribute("ctx", request.getContextPath());
            request.setAttribute("path", request.getRequestURI().substring(request.getContextPath().length()));
            request.setAttribute("siteTitle", siteTitle);
            request.setAttribute("faviconUrl", mediaRevisionResolver.getMediaRevisionUrl(Constants.FAVICON_ICO_PATH));
            request.setAttribute("cssUrl", toRevisionUrl(Constants.ALL_CSS_PATH, staticService.getCssCache()));
            request.setAttribute("jsUrl", toRevisionUrl(Constants.ALL_JS_PATH, staticService.getJsCache()));
            request.setAttribute("treeJsonUrl", toRevisionUrl(Constants.TREE_JSON_PATH, articleService.getTreeJson(accessLevel)));
            request.setAttribute("prefix", prefix);
            request.setAttribute("user", user);
        }
        return true;
    }

    private String toRevisionUrl(String path, RevisionContent revisionContent) {
        return mediaRevisionResolver.toRevisionUrl(path, revisionContent.getRevision());
    }
}