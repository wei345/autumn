package io.liuwei.autumn;

import com.vip.vjtools.vjkit.mapper.JsonMapper;
import io.liuwei.autumn.converter.PageConverter;
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
    private PageConverter pageConverter;

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

    @Cacheable(value = CacheConstants.TREE_JSON)
    public RevisionContent getTreeJson(AccessLevelEnum accessLevel) {
        ArticleTreeNode root = getTreeRoot(accessLevel);
        String json = jsonMapper.toJson(root);
        return RevisionContentUtil.newRevisionContent(json, mediaRevisionResolver);
    }

    @Cacheable(value = CacheConstants.TREE_HTML)
    public String getTreeHtml(AccessLevelEnum accessLevel) {
        ArticleTreeNode root = getTreeRoot(accessLevel);
        StringBuilder stringBuilder = new StringBuilder(10240);
        TreeUtil.buildTreeHtml(root.getChildren(), contextPath, stringBuilder);
        return stringBuilder.toString();
    }

    @Cacheable(value = CacheConstants.TREE_ROOT)
    public ArticleTreeNode getTreeRoot(AccessLevelEnum accessLevel) {
        List<Article> articles = listArticles(accessLevel);
        return TreeUtil.toArticleTree(articles);
    }

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
        ArticleHtml articleHtml = pageConverter.convert(article.getTitle(), article.getContent());
        articleHtml.setToc(HtmlUtil.makeNumberedToc(articleHtml.getToc()));
        articleHtml.setContent(HtmlUtil
                .rewriteImgSrcAppendVersionParam(articleHtml.getContent(), article.getPath(), mediaRevisionResolver));

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
        vo.setTitleHtml(articleHtml.getTitle());
        vo.setContentHtml(articleHtml.getContent());
        vo.setTocHtml(articleHtml.getToc());
        return vo;
    }
}
