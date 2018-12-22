package xyz.liuw.autumn.service;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.Maps;
import com.google.common.hash.Hashing;
import com.vip.vjtools.vjkit.security.CryptoUtil;
import com.vip.vjtools.vjkit.text.EncodeUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.util.CookieGenerator;
import xyz.liuw.autumn.util.WebUtil;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.constraints.NotNull;
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

    private static final User USER_REMEMBER_ME_PARSE_ERROR = new User();

    private static final User USER_NOT_EXIST_OR_PASSWORD_ERROR = new User();

    private static Logger logger = LoggerFactory.getLogger(UserService.class);

    private static ThreadLocal<User> userThreadLocal = new ThreadLocal<>();

    private byte[] aesKey;

    @SuppressWarnings("FieldCanBeLocal")
    private int rememberMeSeconds = 3600 * 24 * 365;

    private Map<String, User> users;

    private Map<Long, User> idToUser;

    @Autowired
    private WebUtil webUtil;

    private Cache<String, User> rememberMeToUserCache = CacheBuilder.newBuilder()
            .expireAfterWrite(30, TimeUnit.SECONDS)
            .maximumSize(10_000)
            .build();

    public static boolean isLogged() {
        return userThreadLocal.get() != null;
    }

    public void setCurrentUser(HttpServletRequest request, HttpServletResponse response) {
        User user = getRememberMeUser(request, response);
        userThreadLocal.set(user);
    }

    /**
     * @return 如果登录成功，则返回 true，否则返回 false
     */
    public boolean login(String username, String plainPassword, HttpServletRequest request, HttpServletResponse response) {
        User user = checkPassword(username, plainPassword);
        if (user == null) {
            return false;
        }
        setRememberMe(user, plainPassword, request, response);
        return true;
    }

    public void logout(HttpServletRequest request, HttpServletResponse response) {
        deleteCookie(REMEMBER_ME_COOKIE_NAME, response);

        // 设置 logout cookie，JavaScript 检查该 cookie，清理客户端用户数据，然后删除该 cookie
        CookieGenerator cg = new CookieGenerator();
        cg.setCookieName(LOGOUT_COOKIE_NAME);
        cg.setCookieMaxAge(-1);
        cg.setCookiePath(webUtil.getContextPath() + "/");
        cg.addCookie(response, String.valueOf(System.currentTimeMillis()));
    }

    private void setRememberMe(User user, String plainPassword, HttpServletRequest request, HttpServletResponse response) {
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
    }

    /**
     * @return 如果 rememberMe 验证成功，返回 User 对象，否则返回 null
     */
    private User getRememberMeUser(HttpServletRequest request, HttpServletResponse response) {
        Cookie cookie = getCookie(REMEMBER_ME_COOKIE_NAME, request);
        if (cookie == null) {
            return null;
        }

        String rememberMe = cookie.getValue();
        if (StringUtils.isBlank(rememberMe)) {
            deleteCookie(cookie.getName(), response);
            return null;
        }

        User user;
        try {
            user = rememberMeToUserCache.get(rememberMe, () -> parseRememberMeForUser(rememberMe));
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        }
        if (user != null && user != USER_NOT_EXIST_OR_PASSWORD_ERROR && user != USER_REMEMBER_ME_PARSE_ERROR) {
            return user;
        }

        deleteCookie(cookie.getName(), response);
        return null;
    }

    /**
     * @param rememberMe 存在登录用户 cookie 里 AES 加密的字符串
     * @return USER_REMEMBER_ME_PARSE_ERROR 或 USER_NOT_EXIST_OR_PASSWORD_ERROR 或正常用户对象
     */
    private @NotNull User parseRememberMeForUser(String rememberMe) {
        try {
            String decrypted = CryptoUtil.aesDecrypt(EncodeUtil.decodeBase64(rememberMe), aesKey);
            StringTokenizer tokenizer = new StringTokenizer(decrypted, SEPARATOR);
            long id = Long.parseLong(tokenizer.nextToken());
            String password = tokenizer.nextToken();
            User user = checkPassword(id, password);
            if (user == null) {
                return USER_NOT_EXIST_OR_PASSWORD_ERROR;
            }
            return user;
        } catch (Exception e) {
            logger.debug(String.format("解析 rememberMe cookie 失败 '%s'", rememberMe), e);
            return USER_REMEMBER_ME_PARSE_ERROR;
        }
    }

    @SuppressWarnings("SameParameterValue")
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

    /**
     * @return 如果 username 和 password 验证通过，则返回 User 对象，否则返回 null
     */
    private User checkPassword(Long id, String password) {
        User user = idToUser.get(id);
        return checkPassword(user, password);
    }

    /**
     * @return 如果 username 和 password 验证通过，则返回 User 对象，否则返回 null
     */
    @VisibleForTesting
    User checkPassword(String username, String password) {
        User user = users.get(username);
        return checkPassword(user, password);
    }

    /**
     * @return 如果 username 和 password 验证通过，则返回 User 对象，否则返回 null
     */
    private User checkPassword(User user, String password) {
        if (user == null) {
            return null;
        }
        String s = password + user.getSalt();
        @SuppressWarnings("UnstableApiUsage") String sha = Hashing.sha512().hashString(s, StandardCharsets.UTF_8).toString();
        if (sha.equals(user.getPassword())) {
            return user;
        } else {
            return null;
        }
    }

    @VisibleForTesting
    User getUser(String username) {
        return users.get(username);
    }

    @Value("${autumn.aes.key}")
    private void setAesKey(String aesKey) {
        this.aesKey = EncodeUtil.decodeHex(aesKey.toUpperCase());
    }

    @VisibleForTesting
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

    static class User {
        private Long id;
        private String username;
        private String password;
        private String salt;

        // used by Jackson deserialize
        public User() {
        }

        public User(long id, String username, String password, String salt) {
            this.id = id;
            this.username = username;
            this.password = password;
            this.salt = salt;
        }

        Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        String getSalt() {
            return salt;
        }

        public void setSalt(String salt) {
            this.salt = salt;
        }
    }
}
