package io.liuwei.autumn.manager;

import com.google.common.collect.Maps;
import io.liuwei.autumn.constant.CacheNames;
import io.liuwei.autumn.dao.DataFileDao;
import io.liuwei.autumn.enums.AccessLevelEnum;
import io.liuwei.autumn.enums.SourceFormatEnum;
import io.liuwei.autumn.model.Article;
import io.liuwei.autumn.model.DataInfo;
import io.liuwei.autumn.model.Media;
import io.liuwei.autumn.util.AsciidocArticleParser;
import io.liuwei.autumn.util.CollectionUtil;
import io.liuwei.autumn.util.DiffUtil;
import io.liuwei.autumn.util.MediaTypeUtil;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.Cache;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author liuwei
 * @since 2021-07-07 16:29
 */
@Component
@Slf4j
public class ArticleManager {

    @Autowired
    private DataFileDao dataFileDao;

    @Autowired
    @Qualifier("mediaCache")
    private Cache mediaCache;

    @Autowired
    @Qualifier("viewCache")
    private Cache viewCache;

    @Autowired
    private AsciidocArticleParser asciidocArticleParser;

    /**
     * 包含数据目录下所有文件，包括文章
     */
    // file path -> File. file path 以 "/" 开头，"/" 表示数据目录
    private volatile Map<String, Media> mediaMap = Collections.emptyMap();

    private volatile Map<String, Article> articleMap = Collections.emptyMap();

    @Getter
    private volatile DataInfo dataInfo;

    @PostConstruct
    public void init() {
        reload();
    }

    @Caching(evict = {
            @CacheEvict(value = CacheNames.ARTICLE_LIST, allEntries = true),
            @CacheEvict(value = CacheNames.ARTICLE_TREE_JSON, allEntries = true),
            @CacheEvict(value = CacheNames.ARTICLE_TREE_HTML, allEntries = true),
    })
    public synchronized DataInfo reload() {
        long startTime = System.currentTimeMillis();

        Map<String, Media> mediaMap1 = mediaMap;
        Map<String, Article> articleMap1 = articleMap;
        Map<String, File> fileMap2 = dataFileDao.getAllFileMap();

        Map<String, Long> map1 = mediaMap1.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, o -> o.getValue().getLastModified()));
        Map<String, Long> map2 = fileMap2.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, o -> o.getValue().lastModified()));
        DiffUtil.Diff<String> diff = DiffUtil.diff(map1, map2);

        if (diff.getAdded().size() == 0 && diff.getModified().size() == 0 && diff.getDeleted().size() == 0) {
            dataInfo.setCheckTime(new Date());
        } else {
            Map<String, Media> mediaMap2 = Maps.newHashMapWithExpectedSize(fileMap2.size());
            Map<String, Article> articleMap2 = Maps.newHashMapWithExpectedSize(fileMap2.size());

            Iterator<String> addedOrModifiedIterator = CollectionUtil.unionIterator(diff.getAdded(), diff.getModified());
            while (addedOrModifiedIterator.hasNext()) {
                String path = addedOrModifiedIterator.next();
                File file = fileMap2.get(path);
                Media media = toMedia(file, path);
                mediaMap2.put(path, media);
                String articlePath = toArticlePath(path);
                if (articlePath != null) {
                    Article article = toArticle(file, articlePath);
                    articleMap2.put(articlePath, article);
                    media.setAccessLevel(article.getAccessLevel());
                }
            }

            for (String path : diff.getNotChanged()) {
                mediaMap2.put(path, mediaMap1.get(path));
                String articlePath = toArticlePath(path);
                if (articlePath != null) {
                    articleMap2.put(articlePath, articleMap1.get(articlePath));
                }
            }

            long costMills = System.currentTimeMillis() - startTime;
            DataInfo dataInfo = toDataInfo(dataFileDao.getDataDir(), mediaMap2, articleMap2, costMills, diff);
            this.mediaMap = mediaMap2;
            this.articleMap = articleMap2;
            this.dataInfo = dataInfo;
            mediaCache.clear();
            viewCache.clear();
            log.info("Data changed. {}", dataInfo);
        }

        return dataInfo;
    }

    private String toArticlePath(String mediaPath) {
        return SourceFormatEnum.getByFileName(mediaPath) == SourceFormatEnum.ASCIIDOC ?
                StringUtils.substringBeforeLast(mediaPath, ".") : null;
    }

    public Media getMedia(String path) {
        return mediaMap.get(path);
    }

    public Article getArticle(String path) {
        return articleMap.get(path);
    }

    @Cacheable(value = CacheNames.ARTICLE_LIST)
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
        media.setMediaType(MediaTypeUtil.getMediaType(file.getName()));
        media.setFile(file);
        media.setLastModified(file.lastModified());
        media.setAccessLevel(AccessLevelEnum.ANON);
        return media;
    }

    private Article toArticle(File file, String path) {
        if (file == null) {
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
                                Long costMills,
                                DiffUtil.Diff<String> diff) {
        Date time = new Date();
        DataInfo dataInfo = new DataInfo();
        dataInfo.setDataDir(dataDir);
        dataInfo.setLoadTime(time);
        dataInfo.setCheckTime(time);
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

        DataInfo.DiffCount diffCount = new DataInfo.DiffCount();
        diffCount.setAdded(diff.getAdded().size());
        diffCount.setModified(diff.getModified().size());
        diffCount.setDeleted(diff.getDeleted().size());
        diffCount.setNotChanged(diff.getNotChanged().size());
        diffCount.setChanged(diffCount.getAdded() + diffCount.getModified() + diffCount.getDeleted());
        dataInfo.setDiffCount(diffCount);
        return dataInfo;
    }

}
