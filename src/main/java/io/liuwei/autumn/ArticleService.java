package io.liuwei.autumn;

import com.vip.vjtools.vjkit.mapper.JsonMapper;
import io.liuwei.autumn.converter.ContentHtmlConverter;
import io.liuwei.autumn.domain.Link;
import io.liuwei.autumn.enums.AccessLevelEnum;
import io.liuwei.autumn.model.*;
import io.liuwei.autumn.util.HtmlUtil;
import io.liuwei.autumn.util.RevisionContentUtil;
import io.liuwei.autumn.util.TreeUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.util.LinkedList;
import java.util.List;

/**
 * @author liuwei
 * @since 2021-07-07 16:30
 */
@Component
public class ArticleService {

    @Autowired
    private ArticleManager articleManager;

    @Autowired
    private ContentHtmlConverter contentHtmlConverter;

    @Autowired
    private MediaRevisionResolver mediaRevisionResolver;

    @Autowired
    private JsonMapper jsonMapper;

    @Value("${server.servlet.context-path}")
    private String contextPath;

    public Media getMedia(String relativePath) {
        return articleManager.getMedia(relativePath);
    }

    public Article getArticle(String path) {
        return articleManager.getArticle(path);
    }

    public List<Article> listArticles(AccessLevelEnum accessLevel) {
        return articleManager.listArticles(accessLevel);
    }

    @Cacheable(CacheConstants.ARTICLE_TREE_JSON)
    public RevisionContent getTreeJson(AccessLevelEnum accessLevel) {
        ArticleTreeNode root = getTreeRoot(accessLevel);
        String json = jsonMapper.toJson(root);
        return RevisionContentUtil.newRevisionContent(json, mediaRevisionResolver);
    }

    @Cacheable(CacheConstants.ARTICLE_TREE_HTML)
    public String getTreeHtml(AccessLevelEnum accessLevel) {
        ArticleTreeNode root = getTreeRoot(accessLevel);
        StringBuilder stringBuilder = new StringBuilder(10240);
        TreeUtil.buildTreeHtml(root.getChildren(), contextPath, stringBuilder);
        return stringBuilder.toString();
    }

    @Cacheable(CacheConstants.ARTICLE_TREE_ROOT)
    public ArticleTreeNode getTreeRoot(AccessLevelEnum accessLevel) {
        List<Article> articles = listArticles(accessLevel);
        return TreeUtil.toArticleTree(articles);
    }

    @Cacheable(CacheConstants.ARTICLE_BREADCRUMB)
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
            return new Link("Home", "/");
        }
        String name = StringUtils.substringAfterLast(path, "/");

        Article article = articleManager.getArticle(path);
        if (article == null) {
            article = articleManager.getArticle(path + "/" + name);
        }

        if (article == null || !article.getAccessLevel().allow(accessLevel)) {
            return new Link(name);
        } else {
            return new Link(article.getTitle(), article.getPath());
        }
    }

    @Cacheable(value = CacheConstants.ARTICLE_VO, key = "#article.path")
    public ArticleVO toVO(Article article) {
        ContentHtml contentHtml = contentHtmlConverter.convert(article.getTitle(), article.getContent());
        contentHtml.setToc(HtmlUtil.makeNumberedToc(contentHtml.getToc()));
        contentHtml.setContent(HtmlUtil
                .rewriteImgSrcAppendVersionParam(contentHtml.getContent(), article.getPath(), mediaRevisionResolver));

        ArticleVO vo = new ArticleVO();
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
        vo.setTitleHtml(contentHtml.getTitle());
        vo.setContentHtml(contentHtml.getContent());
        vo.setTocHtml(contentHtml.getToc());
        return vo;
    }
}
