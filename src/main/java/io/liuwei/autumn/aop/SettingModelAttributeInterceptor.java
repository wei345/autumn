package io.liuwei.autumn.aop;

import io.liuwei.autumn.component.MediaRevisionResolver;
import io.liuwei.autumn.config.AppProperties;
import io.liuwei.autumn.constant.Constants;
import io.liuwei.autumn.enums.AccessLevelEnum;
import io.liuwei.autumn.model.RevisionContent;
import io.liuwei.autumn.model.User;
import io.liuwei.autumn.service.ArticleService;
import io.liuwei.autumn.service.StaticService;
import io.liuwei.autumn.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;

/**
 * @author liuwei
 * @since 2018-12-19
 */
public class SettingModelAttributeInterceptor implements HandlerInterceptor {

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

    private final String prefix;

    public SettingModelAttributeInterceptor(String prefix) {
        this.prefix = prefix;
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
        if (modelAndView != null) {
            Map<String, Object> model = modelAndView.getModel();
            User user = userService.getCurrentUser(request, response);
            AccessLevelEnum accessLevel = userService.getAccessLevel(user);
            model.put("ctx", request.getContextPath());
            model.put("siteTitle", appProperties.getSiteTitle());
            model.put("faviconUrl", mediaRevisionResolver.toRevisionUrl(Constants.FAVICON_DOT_ICO));
            model.put("cssUrl", toRevisionUrl(Constants.CSS_ALL_DOT_CSS, staticService.getAllCss()));
            model.put("jsUrl", toRevisionUrl(Constants.JS_ALL_DOT_JS, staticService.getAllJs()));
            model.put("treeJsonUrl", toRevisionUrl(Constants.TREE_DOT_JSON, articleService.getTreeJson(accessLevel)));
            model.put("prefix", prefix);
            model.put("user", user);
        }
    }

    private String toRevisionUrl(String path, RevisionContent revisionContent) {
        return mediaRevisionResolver.toRevisionUrl(path, revisionContent.getRevision());
    }
}