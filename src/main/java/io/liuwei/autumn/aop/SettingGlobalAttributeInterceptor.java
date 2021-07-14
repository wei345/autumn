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

            // todo 只有 view 页需要设置
            User user = userService.getCurrentUser(request, response);
            AccessLevelEnum accessLevel = userService.getAccessLevel(user);
            request.setAttribute("ctx", request.getContextPath());
            request.setAttribute("siteTitle", appProperties.getSiteTitle());
            request.setAttribute("faviconUrl", mediaRevisionResolver.toRevisionUrl(Constants.FAVICON_DOT_ICO));
            request.setAttribute("cssUrl", toRevisionUrl(Constants.CSS_ALL_DOT_CSS, staticService.getAllCss()));
            request.setAttribute("jsUrl", toRevisionUrl(Constants.JS_ALL_DOT_JS, staticService.getAllJs()));
            request.setAttribute("treeJsonUrl", toRevisionUrl(Constants.TREE_DOT_JSON, articleService.getTreeJson(accessLevel)));
            request.setAttribute("prefix", prefix);
            request.setAttribute("user", user);
        }
        return true;
    }

    private String toRevisionUrl(String path, RevisionContent revisionContent) {
        return mediaRevisionResolver.toRevisionUrl(path, revisionContent.getRevision());
    }
}