package io.liuwei.autumn;

import io.liuwei.autumn.model.Article;
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
        return articleManager.getFileByPath(relativePath);
    }

    public Article getArticle(String path) {
        return articleManager.getArticleByPath(path);
    }
}
