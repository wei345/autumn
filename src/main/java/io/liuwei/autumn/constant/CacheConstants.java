package io.liuwei.autumn.constant;

/**
 * @author liuwei
 * @since 2021-07-08 00:55
 */
public class CacheConstants {
    // -- 使用 @Cacheable --
    public static final String ARTICLE_LIST = "autumn:article:list";
    public static final String ARTICLE_TREE_JSON = "autumn:article:tree:json";
    public static final String ARTICLE_TREE_HTML = "autumn:article:tree:html";

    public static final String ARTICLE_HTML = "autumn:article:html";
    public static final String ARTICLE_BREADCRUMB = "autumn:article:breadcrumb";

    public static final String STATIC = "autumn:static";

    // -- 直接使用 Spring Cache 接口 --
    public static final String ARTICLE_HIT = "autumn:article:hit";
    public static final String MEDIA_REVISION_CONTENT = "autumn:media:revision_content";
    public static final String VIEW_HTML = "autumn:view:html";

    // -- 使用 Guava cache 或 StringRedisTemplate --
    public static final String RATE_LIMIT_LOGIN = "autumn:rate-limit:login:";
    public static final String RATE_LIMIT_SEARCH = "autumn:rate-limit:search:";
}
