package io.liuwei.autumn.service;

import com.vip.vjtools.vjkit.mapper.JsonMapper;
import io.liuwei.autumn.constant.CacheNames;
import io.liuwei.autumn.constant.Constants;
import io.liuwei.autumn.converter.ArticleHtmlConverter;
import io.liuwei.autumn.enums.AccessLevelEnum;
import io.liuwei.autumn.manager.MediaManager;
import io.liuwei.autumn.manager.RevisionContentManager;
import io.liuwei.autumn.model.*;
import io.liuwei.autumn.util.HtmlUtil;
import io.liuwei.autumn.util.TreeUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.List;

/**
 * @author liuwei
 * @since 2021-07-07 16:30
 */
@Component
@Slf4j
public class ArticleService {

    @Autowired
    private MediaManager mediaManager;

    @Autowired
    private RevisionContentManager revisionContentManager;

    @Autowired
    private ArticleHtmlConverter articleHtmlConverter;

    @Autowired
    private JsonMapper jsonMapper;

    @Autowired
    private ArticleService aopProxy;

    @Value("${server.servlet.context-path}")
    private String contextPath;

    public Article getArticle(String path) {
        return mediaManager.getArticle(path);
    }

    public List<Article> listArticles(AccessLevelEnum accessLevel) {
        return mediaManager.listArticles(accessLevel);
    }

    @Cacheable(CacheNames.ARTICLE_TREE_JSON)
    public RevisionContent getTreeJson(AccessLevelEnum accessLevel) {
        TreeNode root = getTree(accessLevel);
        byte[] bytes = jsonMapper.toJson(root).getBytes(StandardCharsets.UTF_8);
        return revisionContentManager.toRevisionContent(bytes, MediaType.APPLICATION_JSON);
    }

    @Cacheable(CacheNames.ARTICLE_TREE_HTML)
    public String getTreeHtml(AccessLevelEnum accessLevel) {
        TreeNode root = getTree(accessLevel);
        StringBuilder builder = new StringBuilder(10240);
        TreeUtil.buildTreeHtml(root.getChildren(), contextPath, builder);
        return builder.toString();
    }

    private TreeNode getTree(AccessLevelEnum accessLevel) {
        return TreeUtil.toArticleTree(listArticles(accessLevel));
    }

    @Cacheable(value = CacheNames.ARTICLE_BREADCRUMB, keyGenerator = "cacheKeyGenerator")
    public List<Link> getBreadcrumbLinks(Article article, AccessLevelEnum accessLevel) {
        LinkedList<Link> links = new LinkedList<>();
        int lastSlash;
        String path = article.getPath();
        while ((lastSlash = path.lastIndexOf('/')) >= 0) {
            String parent = path.substring(0, lastSlash);
            links.addFirst(getBreadcrumbDirectoryLink(parent, accessLevel));
            path = parent;
        }

        if (article.getPath().equals(links.getLast().getHref())) { // e.g. path is / or /a/b/b
            links.getLast().setHref(null);
        } else {
            links.addLast(new Link(article.getTitle()));
        }

        return links;
    }

    /**
     * @param path e.g. /a/b
     */
    private Link getBreadcrumbDirectoryLink(String path, AccessLevelEnum accessLevel) {
        if (path.length() == 0) {
            return new Link(Constants.HOMEPAGE_TITLE, "/");
        }
        String name = StringUtils.substringAfterLast(path, "/");

        Article article = mediaManager.getArticle(path);
        if (article == null) {
            article = mediaManager.getArticle(path + "/" + name);
        }

        if (article == null || !article.getAccessLevel().allow(accessLevel)) {
            return new Link(name);
        } else {
            return new Link(article.getTitle(), article.getPath());
        }
    }

    @Cacheable(value = CacheNames.ARTICLE_HTML, keyGenerator = "cacheKeyGenerator")
    public ArticleHtml getArticleHtml(Article article) {
        ArticleHtml articleHtml = articleHtmlConverter.convert(article.getTitle(), article.getContent());
        articleHtml.setTocHtml(HtmlUtil.toNumberedTocHtml(articleHtml.getTocHtml()));
        articleHtml.setContentHtml(
                HtmlUtil.addHeadingClass(
                        HtmlUtil.rewriteImgSrcToRevisionUrl(
                                articleHtml.getContentHtml(),
                                article.getPath(),
                                revisionContentManager::toRevisionUrl)));
        return articleHtml;
    }

    public ArticleVO toVO(Article article) {
        ArticleHtml articleHtml = aopProxy.getArticleHtml(article);
        ArticleVO vo = new ArticleVO();
        vo.setPath(article.getPath());
        vo.setTitle(article.getTitle());
        vo.setName(article.getName());
        vo.setCreated(article.getCreated());
        vo.setModified(article.getModified());
        vo.setCategory(article.getCategory());
        vo.setTags(article.getTags());
        vo.setAccessLevel(article.getAccessLevel());
        vo.setContent(article.getContent());
        vo.setSource(article.getSource());
        vo.setSourceMd5(article.getSourceMd5());
        vo.setTitleHtml(articleHtml.getTitleHtml());
        vo.setContentHtml(articleHtml.getContentHtml());
        vo.setTocHtml(articleHtml.getTocHtml());
        return vo;
    }
}
