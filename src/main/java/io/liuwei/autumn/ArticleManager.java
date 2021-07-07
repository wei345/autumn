package io.liuwei.autumn;

import io.liuwei.autumn.enums.SourceFormatEnum;
import io.liuwei.autumn.model.Article;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * @author liuwei
 * @since 2021-07-07 16:29
 */
@Component
public class ArticleManager {

    @Autowired
    private DataFileDao dataFileDao;

    private final AsciidocArticleParser asciidocArticleParser = new AsciidocArticleParser();

    public File getFileByPath(String path) {
        return dataFileDao.getByPath(path);
    }

    public Article getArticleByPath(String path) {
        return toArticle(dataFileDao.getArticleFileByPath(path), path);
    }

    private Article toArticle(File file, String relativePath) {
        if (file == null) {
            return null;
        }

        SourceFormatEnum sourceFormat = SourceFormatEnum.getByFileName(file.getName());
        if (sourceFormat != SourceFormatEnum.ASCIIDOC) {
            return null;
        }

        String fileContent;
        try {
            fileContent = FileUtils.readFileToString(file, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        Article article = asciidocArticleParser.parse(fileContent, relativePath);
        if (article.getCreated() == null) {
            article.setCreated(new Date(file.lastModified()));
        }
        if (article.getModified() == null) {
            article.setModified(new Date(file.lastModified()));
        }
        article.setFile(file);
        return article;
    }

}
