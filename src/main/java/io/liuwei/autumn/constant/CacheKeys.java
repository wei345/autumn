package io.liuwei.autumn.constant;

/**
 * @author liuwei602099
 * @since 2021-07-16 10:28
 */
public class CacheKeys {
    // -- 用于 Guava cache 或 StringRedisTemplate --
    public static final String RATE_LIMIT_LOGIN_PREFIX = "autumn:rate-limit:login:";
    public static final String RATE_LIMIT_SEARCH_PREFIX = "autumn:rate-limit:search:";

    // -- 用于 Spring @Cacheable --
    public static final String JS_ALL_DOT_JS = "'" + Constants.JS_ALL_DOT_JS + "'";
    public static final String CSS_ALL_DOT_CSS = "'" + Constants.CSS_ALL_DOT_CSS + "'";
    public static final String HELP = "'" + Constants.HELP + "'";
}
