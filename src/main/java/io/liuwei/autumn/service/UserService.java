package io.liuwei.autumn.service;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.Maps;
import com.google.common.hash.Hashing;
import com.google.common.io.BaseEncoding;
import com.vip.vjtools.vjkit.security.CryptoUtil;
import com.vip.vjtools.vjkit.text.StringBuilderHolder;
import io.liuwei.autumn.enums.AccessLevelEnum;
import io.liuwei.autumn.model.User;
import io.liuwei.autumn.util.WebUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.util.CookieGenerator;

import javax.annotation.PostConstruct;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.constraints.NotNull;
import java.util.Arrays;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static com.vip.vjtools.vjkit.security.CryptoUtil.*;
import static com.vip.vjtools.vjkit.text.EncodeUtil.*;
import static java.nio.charset.StandardCharsets.*;

/**
 * @author liuwei
 * Created by liuwei on 2018/11/22.
 */
@SuppressWarnings({"UnstableApiUsage", "WeakerAccess"})
@Component
public class UserService {

    // 改变这个值会使所有已登录 Cookie 失效
    private static final int REMEMBER_ME_VERSION = 2;
    private static final String LOGOUT_COOKIE_NAME = "logout";
    private static final String SEPARATOR = "|";
    private static final User NULL_USER = new User();
    private static Logger logger = LoggerFactory.getLogger(UserService.class);
    private static ThreadLocal<User> userThreadLocal = new ThreadLocal<>();
    private String rememberMeCookieName;
    private byte[] aesKey;

    @SuppressWarnings("FieldCanBeLocal")
    private int rememberMeSeconds = 3600 * 24 * 365;

    private Map<String, User> users;

    private Map<Long, User> idToUser;

    @Value("${autumn.access.owner-user-id}")
    private Long ownerUserId;

    @Autowired
    private WebUtil webUtil;
    private Cache<String, User> rememberMe2UserCache = CacheBuilder.newBuilder()
            .expireAfterWrite(30, TimeUnit.SECONDS)
            .maximumSize(10_000)
            .build();

    public static boolean isLogged() {
        return userThreadLocal.get() != null;
    }

    @VisibleForTesting
    static byte[] passwordDigest1(String plainPassword, byte[] salt) {
        return Hashing.hmacSha1(salt).hashString(plainPassword, UTF_8).asBytes();
    }

    @VisibleForTesting
    static byte[] passwordDigest2(byte[] passwordDigest1, byte[] salt) {
        return Hashing.hmacSha512(salt).hashBytes(passwordDigest1).asBytes();
    }

    private static byte[] decodeBase64UrlSafe(String base64String) {
        return BaseEncoding.base64Url().decode(base64String);
    }

    private static String encodeBase64UrlSafe(byte[] bytes) {
        return BaseEncoding.base64Url().omitPadding().encode(bytes);
    }

    @PostConstruct
    private void init() {
        rememberMeCookieName = webUtil.getPrefix() + "me";
    }

    public void setCurrentUser(HttpServletRequest request, HttpServletResponse response) {
        User user = getRememberMeUser(request, response);
        userThreadLocal.set(user);
    }

    public User getCurrentUser(HttpServletRequest request, HttpServletResponse response) {
        return getRememberMeUser(request, response);
    }

    public AccessLevelEnum getAccessLevel(User user) {
        if (user == null) {
            return AccessLevelEnum.PUBLIC;
        }
        if (user.getIsOwner()) {
            return AccessLevelEnum.PRIVATE;
        }
        return AccessLevelEnum.USER;
    }

    /**
     * @return 如果登录成功，则返回 true，否则返回 false
     */
    public boolean login(String username, String plainPassword, HttpServletRequest request, HttpServletResponse response) {
        User user = users.get(username);
        if (user == null) {
            return false;
        }
        if (!checkPlainPassword(user, plainPassword)) {
            return false;
        }
        setRememberMe(user, plainPassword, request, response);
        return true;
    }

    public void logout(HttpServletRequest request, HttpServletResponse response) {
        deleteCookie(rememberMeCookieName, request, response);

        // 设置 logout cookie，JavaScript 检查该 cookie，清理客户端用户数据，然后删除该 cookie
        String value = String.valueOf(System.currentTimeMillis());
        CookieGenerator cg = new CookieGenerator();
        cg.setCookieName(LOGOUT_COOKIE_NAME);
        cg.setCookieMaxAge(-1);
        addCookie(cg, value, request, response);
    }

    private void setRememberMe(User user, String plainPassword, HttpServletRequest request, HttpServletResponse response) {
        // version|id|passwordDigest1|timeOnSeconds
        String raw = StringBuilderHolder.getGlobal()
                .append(REMEMBER_ME_VERSION)
                .append(SEPARATOR)
                .append(user.getId())
                .append(SEPARATOR)
                .append(encodeBase64UrlSafe(passwordDigest1(plainPassword, user.getSaltBytes())))
                .append(SEPARATOR)
                .append(System.currentTimeMillis() / 1000)
                .toString();
        String encrypted = encodeBase64UrlSafe(aesEncrypt(raw.getBytes(UTF_8), aesKey));
        CookieGenerator cg = new CookieGenerator();
        cg.setCookieName(rememberMeCookieName);
        cg.setCookieMaxAge(rememberMeSeconds);
        cg.setCookieHttpOnly(true);
        addCookie(cg, encrypted, request, response);
    }

    /**
     * @return 如果 rememberMe 验证成功，返回 User 对象，否则返回 null
     */
    private User getRememberMeUser(HttpServletRequest request, HttpServletResponse response) {
        Cookie cookie = getCookie(rememberMeCookieName, request);
        if (cookie == null) {
            return null;
        }

        String rememberMe = cookie.getValue();
        if (StringUtils.isBlank(rememberMe)) {
            deleteCookie(cookie.getName(), request, response);
            return null;
        }

        User user;
        try {
            user = rememberMe2UserCache.get(rememberMe, () -> {
                User user1 = parseRememberMeForUser(rememberMe);
                return user1 == null ? NULL_USER : user1;
            });
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        }
        if (user != null && user != NULL_USER) {
            user.setIsOwner(user.getId().equals(ownerUserId));
            return user;
        }

        deleteCookie(cookie.getName(), request, response);
        return null;
    }

    /**
     * @param rememberMe 存在登录用户 cookie 里 AES 加密的字符串
     * @return USER_REMEMBER_ME_PARSE_ERROR 或 USER_NOT_EXIST_OR_PASSWORD_ERROR 或正常用户对象
     */
    private @NotNull User parseRememberMeForUser(String rememberMe) {
        try {
            String decrypted = CryptoUtil.aesDecrypt(decodeBase64UrlSafe(rememberMe), aesKey);
            StringTokenizer tokenizer = new StringTokenizer(decrypted, SEPARATOR);
            int version = Integer.parseInt(tokenizer.nextToken());
            if (version != REMEMBER_ME_VERSION) {
                logger.info("rememberMe version not match, current: '{}', cookie: '{}'", REMEMBER_ME_VERSION, version);
                return null;
            }
            long id = Long.parseLong(tokenizer.nextToken());
            String password = tokenizer.nextToken();
            User user = idToUser.get(id);
            if (user == null) {
                return null;
            }
            if (!checkRememberMePassword(user, decodeBase64UrlSafe(password))) {
                return null;
            }
            return user;
        } catch (Exception e) {
            logger.debug(String.format("解析 rememberMe cookie 失败 '%s'", rememberMe), e);
            return null;
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

    private void deleteCookie(String name, HttpServletRequest request, HttpServletResponse response) {
        CookieGenerator cg = new CookieGenerator();
        cg.setCookieName(name);
        cg.setCookieMaxAge(0);
        cg.setCookieHttpOnly(true);
        addCookie(cg, null, request, response);
    }

    private void addCookie(CookieGenerator cg, String value, HttpServletRequest request, HttpServletResponse response) {
        cg.setCookiePath(webUtil.getContextPath() + "/");
        cg.setCookieSecure(WebUtil.isSecure(request));
        cg.addCookie(response, value);
    }

    private boolean checkRememberMePassword(User user, byte[] passwordDigest1) {
        return checkPasswordDigest1(user, passwordDigest1);
    }

    @VisibleForTesting
    boolean checkPlainPassword(User user, String plainPassword) {
        return checkPasswordDigest1(user, passwordDigest1(plainPassword, user.getSaltBytes()));
    }

    private boolean checkPasswordDigest1(User user, byte[] passwordDigest1) {
        return Arrays.equals(user.getPasswordBytes(), passwordDigest2(passwordDigest1, user.getSaltBytes()));
    }

    @VisibleForTesting
    User getUser(String username) {
        return users.get(username);
    }

    @Value("${autumn.remember-me.aes-key}")
    private void setAesKey(String aesKey) {
        this.aesKey = decodeHex(aesKey);
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
            User user = new User(id, username, password, salt, decodeHex(password), decodeHex(salt));
            Validate.isTrue(users.get(username) == null, "Duplicate username '%s'", username);
            Validate.isTrue(idToUser.get(id) == null, "Duplicate id '%s'", id);
            users.put(username, user);
            idToUser.put(id, user);
        }
    }

}
