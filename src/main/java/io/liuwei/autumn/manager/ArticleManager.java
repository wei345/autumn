package io.liuwei.autumn.manager;

import com.google.common.collect.Maps;
import io.liuwei.autumn.constant.CacheConstants;
import io.liuwei.autumn.dao.DataFileDao;
import io.liuwei.autumn.enums.AccessLevelEnum;
import io.liuwei.autumn.enums.SourceFormatEnum;
import io.liuwei.autumn.model.Article;
import io.liuwei.autumn.model.DataInfo;
import io.liuwei.autumn.model.Media;
import lombok.Getter;
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

    @Autowired
    private AsciidocArticleParser asciidocArticleParser;

    @Autowired
    private DataFileDao dataFileDao;

    /**
     * 包含数据目录下所有文件，包括文章
     */
    // file path -> File. file path 以 "/" 开头，"/" 表示数据目录
    private volatile Map<String, Media> mediaMap;

    private volatile Map<String, Article> articleMap;

    @Getter
    private volatile DataInfo dataInfo;

    @PostConstruct
    public void init() {
        reload();
    }

    @Caching(evict = {
            @CacheEvict(value = CacheConstants.ARTICLE_LIST, allEntries = true),
            @CacheEvict(value = CacheConstants.ARTICLE_TREE_JSON, allEntries = true),
            @CacheEvict(value = CacheConstants.ARTICLE_TREE_HTML, allEntries = true),
    })
    public synchronized DataInfo reload() {
        long startTime = System.currentTimeMillis();

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

        long costMills = System.currentTimeMillis() - startTime;
        DataInfo dataInfo = toDataInfo(dataFileDao.getDataDir(), mediaMap, articleMap, costMills);
        this.mediaMap = mediaMap;
        this.articleMap = articleMap;
        this.dataInfo = dataInfo;
        log.info("Reloaded. {}", dataInfo);
        return dataInfo;
    }

    public Media getMedia(String path) {
        return mediaMap.get(path);
    }

    public Article getArticle(String path) {
        return articleMap.get(path);
    }

    @Cacheable(value = CacheConstants.ARTICLE_LIST)
    public List<Article> listArticles(AccessLevelEnum accessLevel) {
        if (accessLevel == AccessLevelEnum.OWNER) {
            return new ArrayList<>(articleMap.values());
        }
        return articleMap
                .values()
                .stream()
                .filter(o -> o.getAccessLevel().allow(accessLevel))
                .collect(Collectors.toList());
    }

    private Media toMedia(File file, String path) {
        Media media = new Media();
        media.setPath(path);
        media.setFile(file);
        media.setAccessLevel(AccessLevelEnum.ANON);
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
        article.setLastModified(file.lastModified());
        article.setFile(file);
        return article;
    }

    private DataInfo toDataInfo(String dataDir,
                                Map<String, Media> mediaMap,
                                Map<String, Article> articleMap,
                                Long costMills) {
        DataInfo dataInfo = new DataInfo();
        dataInfo.setDataDir(dataDir);
        dataInfo.setTime(new Date());
        dataInfo.setCost(costMills);
        dataInfo.setFile(mediaMap.size());
        dataInfo.setArticle(articleMap.size());
        dataInfo.setOwnerOnlyAccessible(
                (int) articleMap
                        .values()
                        .stream()
                        .filter(o -> o.getAccessLevel() == AccessLevelEnum.OWNER)
                        .count());
        dataInfo.setUserAccessible(
                (int) articleMap
                        .values()
                        .stream()
                        .filter(o -> o.getAccessLevel().allow(AccessLevelEnum.USER))
                        .count());
        dataInfo.setAnonAccessible(
                (int) articleMap
                        .values()
                        .stream()
                        .filter(o -> o.getAccessLevel().allow(AccessLevelEnum.ANON))
                        .count());
        return dataInfo;
    }

}
