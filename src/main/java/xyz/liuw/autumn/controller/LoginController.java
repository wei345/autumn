package xyz.liuw.autumn.controller;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import xyz.liuw.autumn.service.RateLimitService;
import xyz.liuw.autumn.service.TemplateService;
import xyz.liuw.autumn.service.UserService;
import xyz.liuw.autumn.util.WebUtil;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;

import static org.springframework.web.util.HtmlUtils.htmlEscape;

/**
 * @author liuwei
 * Created by liuwei on 2018/11/22.
 */
@Controller
public class LoginController {

    @Autowired
    private UserService userService;

    @Autowired
    private RateLimitService rateLimitService;

    @Autowired
    private TemplateService templateService;

    @RequestMapping(value = "/login", method = RequestMethod.GET)
    public String login(Map<String, Object> model) {
        templateService.setCtx(model);
        return "login";
    }

    @RequestMapping(value = "/login", method = RequestMethod.POST)
    public String loginSubmit(String username,
                              String password,
                              String ret,
                              Map<String, Object> model,
                              HttpServletRequest request,
                              HttpServletResponse response) {
        templateService.setCtx(model);

        String clientIp = WebUtil.getClientIpAddress(request);
        if (!rateLimitService.acquireLogin(clientIp)) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            model.put("message", "稍后再试");
            model.put("username", htmlEscape(username));
            return "login";
        }

        if (StringUtils.isNotBlank(username)
                && username.length() <= 32
                && StringUtils.isNotBlank(password)
                && password.length() <= 32) {

            if (userService.login(username, password, request, response)) {
                // 登录成功
                return (ret != null && ret.startsWith("/")) ? "redirect:" + ret : "redirect:/";
            }
        }

        // 登录失败
        model.put("message", "用户名或密码错误");
        model.put("username", htmlEscape(username));
        response.setStatus(401);
        return "login";
    }

    @RequestMapping(value = "/logout", method = RequestMethod.GET)
    public String logout(HttpServletRequest request, HttpServletResponse response) {
        userService.logout(request, response);
        return "redirect:/";
    }

}
