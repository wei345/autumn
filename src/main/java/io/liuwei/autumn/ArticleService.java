package io.liuwei.autumn;

import com.vip.vjtools.vjkit.mapper.JsonMapper;
import io.liuwei.autumn.converter.PageConverter;
import io.liuwei.autumn.enums.AccessLevelEnum;
import io.liuwei.autumn.model.*;
import io.liuwei.autumn.util.HtmlUtil;
import io.liuwei.autumn.util.RevisionContentUtil;
import io.liuwei.autumn.util.TreeUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

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
