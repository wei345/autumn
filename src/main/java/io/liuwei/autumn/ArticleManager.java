package io.liuwei.autumn;

import com.google.common.collect.Maps;
import com.vip.vjtools.vjkit.mapper.JsonMapper;
import io.liuwei.autumn.enums.AccessLevelEnum;
import io.liuwei.autumn.enums.SourceFormatEnum;
import io.liuwei.autumn.model.Article;
import io.liuwei.autumn.model.ArticleTreeNode;
import io.liuwei.autumn.model.Media;
import io.liuwei.autumn.model.RevisionContent;
import io.liuwei.autumn.util.RevisionContentUtil;
import io.liuwei.autumn.util.TreeUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author liuwei
 * @since 2021-07-07 16:29
 */
@Component
@Slf4j
public class ArticleManager {

    private final AsciidocArticleParser asciidocArticleParser = new AsciidocArticleParser();
    @Autowired
    private DataFileDao dataFileDao;

    @Autowired
    private JsonMapper jsonMapper;

    @Autowired
    private MediaRevisionResolver mediaRevisionResolver;

    // file path -> File. file path 以 "/" 开头，"/" 表示数据目录
    private volatile Map<String, Media> mediaMap;

    private volatile Map<String, Article> articleMap;

    @PostConstruct
    public void init() throws IOException {
        reload();
    }

    @Caching(evict = {
            @CacheEvict(value = CacheConstants.ARTICLES),
            @CacheEvict(value = CacheConstants.TREE_JSON),
            @CacheEvict(value = CacheConstants.ARTICLE_VO),
    })
    public synchronized void reload() throws IOException {
        Map<String, File> allFileMap = dataFileDao.getAllFileMap();

        Map<String, Media> mediaMap = Maps.newHashMapWithExpectedSize(allFileMap.size());
        Map<String, Article> articleMap = Maps.newHashMapWithExpectedSize(allFileMap.size());
        for (Map.Entry<String, File> fileEntry : allFileMap.entrySet()) {
            Media media = toMedia(fileEntry.getValue(), fileEntry.getKey());
            mediaMap.put(media.getPath(), media);
            if (SourceFormatEnum.getByFileName(fileEntry.getKey()) == SourceFormatEnum.ASCIIDOC) {
                String path = StringUtils.substringBeforeLast(fileEntry.getKey(), ".");
                Article article = toArticle(fileEntry.getValue(), path);
                articleMap.put(path, article);
                media.setAccessLevel(article.getAccessLevel());
            }
        }
        log.info("found {} article", articleMap.size());
        this.mediaMap = mediaMap;
        this.articleMap = articleMap;
    }


    public Media getMedia(String path) {
        return mediaMap.get(path);
    }

    public Article getArticle(String path) {
        return articleMap.get(path);
    }

    @Cacheable(value = CacheConstants.ARTICLES)
    public List<Article> listArticles(AccessLevelEnum accessLevel) {
        if (accessLevel == AccessLevelEnum.OWNER) {
            return new ArrayList<>(articleMap.values());
        }
        return articleMap
                .values()
                .stream()
                .filter(o -> o.getAccessLevel().getLevel() >= accessLevel.getLevel())
                .collect(Collectors.toList());
    }

    @Cacheable(value = CacheConstants.TREE_JSON)
    public RevisionContent getTreeJson(AccessLevelEnum accessLevel) {
        List<Article> articles = listArticles(accessLevel);
        ArticleTreeNode articleTree = TreeUtil.toArticleTree(articles);
        String json = jsonMapper.toJson(articleTree);
        return RevisionContentUtil.newRevisionContent(json, mediaRevisionResolver);
    }

    private Media toMedia(File file, String path) {
        Media media = new Media();
        media.setPath(path);
        media.setFile(file);
        media.setAccessLevel(AccessLevelEnum.PUBLIC);
        return media;
    }

    private Article toArticle(File file, String path) {
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

        Article article = asciidocArticleParser.parse(fileContent, path);
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
