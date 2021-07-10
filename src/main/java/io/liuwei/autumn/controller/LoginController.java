package io.liuwei.autumn.controller;

import io.liuwei.autumn.constant.CacheConstants;
import io.liuwei.autumn.service.UserService;
import io.liuwei.autumn.util.RateLimiter;
import io.liuwei.autumn.util.WebUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

import javax.annotation.PostConstruct;
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

    @Autowired(required = false)
    private StringRedisTemplate stringRedisTemplate;

    private RateLimiter rateLimiter;

    @PostConstruct
    public void init() {
        this.rateLimiter = new RateLimiter(3, 86400, stringRedisTemplate);
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @PostMapping("/login")
    public String loginSubmit(String username,
                              String password,
                              String returnUrl,
                              Map<String, Object> model,
                              HttpServletRequest request,
                              HttpServletResponse response) {

        String rateKey = CacheConstants.RATE_LIMIT_LOGIN + WebUtil.getClientIpAddress(request);
        if (!rateLimiter.acquire(rateKey)) {
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
                return (returnUrl != null && returnUrl.startsWith("/")) ? "redirect:" + returnUrl : "redirect:/";
            }
        }

        // 登录失败
        model.put("message", "用户名或密码错误");
        model.put("username", htmlEscape(username));
        response.setStatus(401);
        return "login";
    }

    @GetMapping("/logout")
    public String logout(HttpServletRequest request, HttpServletResponse response) {
        userService.logout(request, response);
        return "redirect:/";
    }

}
