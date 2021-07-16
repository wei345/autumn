package io.liuwei.autumn.constant;

/**
 * @author liuwei
 * @since 2021-07-08 00:55
 */
public class CacheNames {
    // -- 用于 Spring @Cacheable --
    public static final String ARTICLE_LIST = "autumn:article:list";
    public static final String ARTICLE_TREE_JSON = "autumn:article:tree:json";
    public static final String ARTICLE_TREE_HTML = "autumn:article:tree:html";

    public static final String ARTICLE_HTML = "autumn:article:html";
    public static final String ARTICLE_BREADCRUMB = "autumn:article:breadcrumb";

    public static final String STATIC = "autumn:static";

    // -- 用于独立的 Spring Cache --
    public static final String ARTICLE_HIT = "autumn:article:hit";
    public static final String MEDIA_CONTENT = "autumn:media:content";
    public static final String VIEW_HTML = "autumn:view:html";

}
