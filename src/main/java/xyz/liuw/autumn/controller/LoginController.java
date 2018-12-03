package xyz.liuw.autumn.controller;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.Lists;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import xyz.liuw.autumn.domain.User;
import xyz.liuw.autumn.service.SecurityService;
import xyz.liuw.autumn.service.UserService;
import xyz.liuw.autumn.util.WebUtil;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static org.springframework.web.util.HtmlUtils.htmlEscape;

/**
 * @author liuwei
 * Created by liuwei on 2018/11/22.
 */
@Controller
public class LoginController {

    private static final Logger logger = LoggerFactory.getLogger(LoginController.class);
    private int maxFailures = 10;
    private int failuresRememberSeconds = 86400;
    @Autowired
    private UserService userService;
    @Autowired
    private SecurityService securityService;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    private LoginToken loginToken;
    @Autowired
    private WebUtil webUtil;

    @PostConstruct
    private void init() {
        loginToken = new RedisLoginToken();
    }

    @RequestMapping(value = "/logout", method = RequestMethod.GET)
    public String logout(HttpServletRequest request, HttpServletResponse response) {
        securityService.logout(request, response);
        return "redirect:/";
    }

    @RequestMapping(value = "/login", method = RequestMethod.GET)
    public String login(Map<String, Object> model) {
        webUtil.setCtx(model);
        return "login";
    }

    // 确保检查和更新失败次数线程安全：
    // 每 ip 每秒只允许一次登录尝试。
    // 或每 ip 一个 lock，lock cache 10 秒。
    // 或用 token，初始 maxFailures 个 token，进入时获取 token，登录成功归还 token。（当前采用）
    // 或者用 synchronized，最简单的方式，粒度有点粗。
    @RequestMapping(value = "/login", method = RequestMethod.POST)
    public String loginSubmit(String username, String password, String ret, Map<String, Object> model,
                              HttpServletRequest request, HttpServletResponse response) {
        webUtil.setCtx(model);

        // 检查客户端 IP 失败次数，如果达到阀值则一段时间内禁止登录
        String clientIp = WebUtil.getClientIpAddress(request);
        if (!loginToken.acquire(clientIp)) {
            model.put("message", "稍后再试");
            model.put("username", username);
            response.setStatus(401);
            return "login";
        }

        User user = userService.checkPassword(username, password);

        // 登录成功
        if (user != null) {
            loginToken.success(clientIp);
            securityService.setRememberMe(user, password, request, response);
            return (ret != null && ret.startsWith("/")) ? "redirect:" + ret : "redirect:/";
        }

        // 登录失败
        loginToken.fail(clientIp);
        model.put("message", "用户名或密码错误");
        model.put("username", htmlEscape(username));
        response.setStatus(401);
        return "login";
    }

    interface LoginToken {
        boolean acquire(String clientIp);

        default void success(String clientIp) {
        }

        default void fail(String clientIp) {
        }
    }

    class RedisLoginToken implements LoginToken {
        private static final String PREFIX = "autumn:login:failures:";
        @SuppressWarnings("unchecked")
        private ValueOperations<String, String> valueOperations = stringRedisTemplate.opsForValue();

        private String redisIncrScript =
                "local key = KEYS[1] " +
                        "local current = tonumber(redis.call('incr', key)) " +
                        "if current == 1 then " +
                        "   redis.call('expire', key, '"+ failuresRememberSeconds +"') " +
                        "end " +
                        "return current ";

        private RedisScript<Long> redisScript = new DefaultRedisScript<>(redisIncrScript, Long.class);

        @Override
        public boolean acquire(String clientIp) {
            String key = PREFIX + clientIp;
            String value = valueOperations.get(key);
            if (value == null) {
                return true;
            }
            return Integer.parseInt(value) < maxFailures;
        }

        @Override
        public void fail(String clientIp) {
            String key = PREFIX + clientIp;
            stringRedisTemplate.execute(redisScript, Collections.singletonList(key));
        }
    }

    class MemoryLoginToken implements LoginToken {
        private Cache<String, Integer> cache = CacheBuilder
                .newBuilder()
                .maximumSize(100_000_000)
                .expireAfterWrite(failuresRememberSeconds, TimeUnit.SECONDS)
                .build();

        @Override
        public synchronized boolean acquire(String clientIp) {
            Integer remaining = cache.getIfPresent(clientIp);
            if (remaining == null) {
                cache.put(clientIp, maxFailures - 1);
                return true;
            }
            if (remaining > 0) {
                cache.put(clientIp, remaining - 1);
                return true;
            } else {
                return false;
            }
        }

        @Override
        public synchronized void success(String clientIp) {
            Integer remaining = cache.getIfPresent(clientIp);
            if (remaining == null) {
                return;
            }
            remaining++;
            if (remaining >= maxFailures) {
                cache.invalidate(clientIp);
            } else {
                cache.put(clientIp, remaining);
            }
        }
    }


}
