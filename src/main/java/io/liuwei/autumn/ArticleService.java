package io.liuwei.autumn;

import io.liuwei.autumn.converter.PageConverter;
import io.liuwei.autumn.enums.AccessLevelEnum;
import io.liuwei.autumn.model.*;
import io.liuwei.autumn.util.HtmlUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

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

    public Media getMedia(String relativePath) {
        return articleManager.getMedia(relativePath);
    }

    public Article getArticle(String path) {
        return articleManager.getArticle(path);
    }

    public RevisionContent getTreeJson(AccessLevelEnum accessLevel) {
        return articleManager.getTreeJson(accessLevel);
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
