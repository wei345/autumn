package io.liuwei.autumn.aop;

import io.liuwei.autumn.component.MediaRevisionResolver;
import io.liuwei.autumn.config.AppProperties;
import io.liuwei.autumn.constant.Constants;
import io.liuwei.autumn.enums.AccessLevelEnum;
import io.liuwei.autumn.manager.RevisionContentManager;
import io.liuwei.autumn.model.RevisionContent;
import io.liuwei.autumn.model.User;
import io.liuwei.autumn.service.ArticleService;
import io.liuwei.autumn.service.StaticService;
import io.liuwei.autumn.service.UserService;
import io.liuwei.autumn.util.StringUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * @author liuwei
 * @since 2018-12-19
 */
public class CommonModelAttributeInterceptor implements HandlerInterceptor {

    @Autowired
    private RevisionContentManager revisionContentManager;

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

    private final Pattern copyrightPlaceholderPattern = Pattern.compile("\\{(.+?)\\}");

    public CommonModelAttributeInterceptor(String prefix) {
        this.prefix = prefix;
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
        if (modelAndView != null
                && modelAndView.getViewName() != null
                && !modelAndView.getViewName().startsWith("redirect:")) {
            Map<String, Object> model = modelAndView.getModel();
            User user = userService.getCurrentUser(request, response);
            AccessLevelEnum accessLevel = userService.getAccessLevel(user);
            model.put("ctx", request.getContextPath());
            model.put("siteTitle", appProperties.getSiteTitle());
            model.put("faviconUrl", revisionContentManager.toRevisionUrl(Constants.FAVICON_DOT_ICO));
            model.put("cssUrl", toRevisionUrl(Constants.CSS_ALL_DOT_CSS, staticService.getAllCss()));
            model.put("jsUrl", toRevisionUrl(Constants.JS_ALL_DOT_JS, staticService.getAllJs()));
            model.put("treeJsonUrl", toRevisionUrl(Constants.TREE_DOT_JSON, articleService.getTreeJson(accessLevel)));
            model.put("prefix", prefix);
            model.put("user", user);
            model.put("copyright", getCopyright());
        }
    }

    String getCopyright() {
        Map<String, Object> copyrightCtx = Collections.singletonMap("year", Calendar.getInstance().get(Calendar.YEAR));
        return StringUtil.replaceAll(appProperties.getCopyrightTemplate(), copyrightPlaceholderPattern,
                matcher -> {
                    String k = matcher.group(1).toLowerCase();
                    return copyrightCtx.getOrDefault(k, matcher.group()).toString();
                });
    }

    private String toRevisionUrl(String path, RevisionContent revisionContent) {
        return mediaRevisionResolver.toRevisionUrl(path, revisionContent.getRevision());
    }
}