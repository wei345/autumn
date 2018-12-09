package xyz.liuw.autumn.service;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.Maps;
import com.vip.vjtools.vjkit.mapper.JsonMapper;
import com.vip.vjtools.vjkit.security.CryptoUtil;
import com.vip.vjtools.vjkit.text.EncodeUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.DigestUtils;
import org.springframework.web.util.CookieGenerator;
import xyz.liuw.autumn.domain.User;
import xyz.liuw.autumn.util.WebUtil;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * @author liuwei
 * Created by liuwei on 2018/11/22.
 */
@Component
public class UserService {

    private static final String REMEMBER_ME_COOKIE_NAME = "ME";

    private static final String LOGOUT_COOKIE_NAME = "logout";

    private static final String SEPARATOR = "|";

    private static final String SESSION_USER_KEY = "user";

    private static final User NULL_USER = new User();

    private static Logger logger = LoggerFactory.getLogger(UserService.class);

    private static ThreadLocal<User> userThreadLocal = new ThreadLocal<>();

    private byte[] aesKey;

    @SuppressWarnings("FieldCanBeLocal")
    private int rememberMeSeconds = 3600 * 24 * 365;

    private Map<String, User> users;

    private Map<Long, User> idToUser;

    private Cache<String, User> sessionUserCache = CacheBuilder.newBuilder()
            .expireAfterWrite(30, TimeUnit.SECONDS)
            .maximumSize(1_000_000)
            .build();

    @Autowired
    private JsonMapper jsonMapper;

    @Autowired
    private WebUtil webUtil;

    // 获得当前用户: ThreadLocal User > Cache Session User > Session User > RememberMe

    public static boolean isLogged() {
        return getCurrentUser() != null;
    }

    private static User getCurrentUser() {
        return userThreadLocal.get();
    }

    @Value("${autumn.aes.key}")
    private void setAesKey(String aesKey) {
        this.aesKey = EncodeUtil.decodeHex(aesKey.toUpperCase());
    }

    public void setCurrentUser(HttpServletRequest request, HttpServletResponse response) {
        User user = getRequestUser(request, response);
        userThreadLocal.set(user);
    }

    private User getRequestUser(HttpServletRequest request, HttpServletResponse response) {
        User user = getSessionUser(request.getSession());
        if (user == null) {
            user = getRememberMe(request, response);
            if (user != null) {
                setSessionUser(user, request.getSession());
            }
        }
        return user;
    }

    private void setSessionUser(User user, HttpSession session) {
        User u = new User();
        u.setId(user.getId());
        u.setUsername(user.getUsername());
        String json = jsonMapper.toJson(u);
        sessionUserCache.put(session.getId(), u);
        session.setAttribute(SESSION_USER_KEY, json);
    }

    private User getSessionUser(HttpSession session) {
        User user;
        try {
            user = sessionUserCache.get(session.getId(), () -> {
                String json = (String) session.getAttribute(SESSION_USER_KEY);
                if (StringUtils.isBlank(json)) {
                    return NULL_USER;
                }
                User u = jsonMapper.fromJson(json, User.class);
                if (u == null || u.getId() == null || StringUtils.isBlank(u.getUsername())) {
                    logger.info("delete invalid session user '{}'", json);
                    session.removeAttribute(SESSION_USER_KEY);
                    return NULL_USER;
                }
                return u;
            });
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        }
        return user == NULL_USER ? null : user;
    }

    private User getRememberMe(HttpServletRequest request, HttpServletResponse response) {
        Cookie cookie = getCookie(REMEMBER_ME_COOKIE_NAME, request);
        if (cookie == null) {
            return null;
        }
        String value = cookie.getValue();
        if (StringUtils.isNotBlank(value)) {
            try {
                String decrypted = CryptoUtil.aesDecrypt(EncodeUtil.decodeBase64(value), aesKey);
                StringTokenizer tokenizer = new StringTokenizer(decrypted, SEPARATOR);
                long id = Long.parseLong(tokenizer.nextToken());
                String password = tokenizer.nextToken();
                User user = checkPassword(id, password);
                if (user != null) {
                    return user;
                }
            } catch (Exception e) {
                logger.debug(String.format("解析 rememberMe cookie 失败 '%s'", value), e);
            }
        }
        // 如果 cookie 无效或解析失败，删除 cookie
        deleteCookie(cookie.getName(), response);
        return null;
    }

    private Cookie getCookie(String name, HttpServletRequest request) {
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if (cookie.getName().equals(name)) {
                    return cookie;
                }
            }
        }
        return null;
    }

    private void deleteCookie(String name, HttpServletResponse response) {
        CookieGenerator cg = new CookieGenerator();
        cg.setCookieName(name);
        cg.setCookieMaxAge(0);
        cg.setCookieHttpOnly(true);
        cg.setCookiePath(webUtil.getContextPath() + "/");
        cg.addCookie(response, null);
    }

    public void setRememberMe(User user, String plainPassword, HttpServletRequest request, HttpServletResponse response) {
        // id|password|timeOnSeconds
        String raw = user.getId() + SEPARATOR + plainPassword + SEPARATOR + System.currentTimeMillis() / 1000;
        String encrypted = EncodeUtil.encodeBase64(
                CryptoUtil.aesEncrypt(raw.getBytes(StandardCharsets.UTF_8), aesKey));
        CookieGenerator cg = new CookieGenerator();
        cg.setCookieName(REMEMBER_ME_COOKIE_NAME);
        cg.setCookieMaxAge(rememberMeSeconds);
        cg.setCookieHttpOnly(true);
        cg.setCookiePath(webUtil.getContextPath() + "/");
        cg.addCookie(response, encrypted);

        setSessionUser(user, request.getSession());
    }

    public void logout(HttpServletRequest request, HttpServletResponse response) {
        removeSessionUser(request.getSession());
        deleteCookie(REMEMBER_ME_COOKIE_NAME, response);
    }

    private void removeSessionUser(HttpSession session) {
        sessionUserCache.invalidate(session.getId());
        session.removeAttribute(SESSION_USER_KEY);
    }

    @Value("${autumn.users}")
    void setUsers(String input) {
        Validate.notBlank(input, "config 'autumn.users' is blank");
        users = Maps.newHashMapWithExpectedSize(2);
        idToUser = Maps.newHashMapWithExpectedSize(2);
        for (String s : input.trim().split("\\s*;\\s*")) {
            if (StringUtils.isBlank(s)) {
                continue;
            }
            String[] parts = s.split("\\s+");
            long id = Long.valueOf(parts[0]);
            String username = parts[1];
            String password = parts[2];
            String salt = parts[3];
            User user = new User(id, username, password, salt);
            Validate.isTrue(users.get(username) == null, "Duplicate username '%s'", username);
            Validate.isTrue(idToUser.get(id) == null, "Duplicate id '%s'", id);
            users.put(username, user);
            idToUser.put(id, user);
        }
    }

    User checkPassword(Long id, String password) {
        User user = idToUser.get(id);
        return checkPassword(user, password);
    }

    public User checkPassword(String username, String password) {
        User user = users.get(username);
        return checkPassword(user, password);
    }

    private User checkPassword(User user, String password) {
        if (user == null) {
            return null;
        }
        String s = password + user.getSalt();
        String md5 = DigestUtils.md5DigestAsHex(s.getBytes(StandardCharsets.UTF_8));
        if (md5.equals(user.getPassword())) {
            return user;
        } else {
            return null;
        }
    }

    public User getUser(Long id) {
        return idToUser.get(id);
    }

    User getUser(String username) {
        return users.get(username);
    }

}
