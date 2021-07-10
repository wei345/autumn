package io.liuwei.autumn.aop;

import io.liuwei.autumn.service.ArticleService;
import io.liuwei.autumn.constant.Constants;
import io.liuwei.autumn.component.MediaRevisionResolver;
import io.liuwei.autumn.config.AppProperties;
import io.liuwei.autumn.enums.AccessLevelEnum;
import io.liuwei.autumn.model.RevisionContent;
import io.liuwei.autumn.model.User;
import io.liuwei.autumn.service.StaticService;
import io.liuwei.autumn.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @author liuwei
 * @since 2018-12-19
 */
public class SettingGlobalAttributeInterceptor implements HandlerInterceptor {

    private static final String ALREADY_HANDLED_ATTRIBUTE_NAME = SettingGlobalAttributeInterceptor.class.getName() + ".handled";

    @Autowired
    private UserService userService;

    @Autowired
    private StaticService staticService;

    @Autowired
    private ArticleService articleService;

    @Autowired
    private MediaRevisionResolver mediaRevisionResolver;

    @Autowired
    private AppProperties appProperties;

    private String prefix;

    public SettingGlobalAttributeInterceptor(String prefix) {
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
            request.setAttribute("siteTitle", appProperties.getSiteTitle());
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