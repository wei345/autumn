package io.liuwei.autumn;

import io.liuwei.autumn.enums.AccessLevelEnum;
import io.liuwei.autumn.model.Article;
import io.liuwei.autumn.model.ArticleVO;
import io.liuwei.autumn.model.RevisionContent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;

/**
 * @author liuwei
 * @since 2021-07-07 16:30
 */
@Component
public class ArticleService {

    @Autowired
    private ArticleManager articleManager;

    public File getFile(String relativePath) {
        return articleManager.getMediaFile(relativePath);
    }

    public Article getArticle(String path) {
        return articleManager.getArticleByPath(path);
    }

    public RevisionContent getTreeJson(AccessLevelEnum accessLevel) {
        return articleManager.getTreeJson(accessLevel);
    }

    public boolean checkAccessLevel(Article article, AccessLevelEnum accessLevel) {
        return article.getAccessLevel().getLevel() >= accessLevel.getLevel();
    }

    public ArticleVO toVO(Article article) {
        return articleManager.toVO(article);
    }
}
