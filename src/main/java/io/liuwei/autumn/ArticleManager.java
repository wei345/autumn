package io.liuwei.autumn;

import com.google.common.collect.Maps;
import com.vip.vjtools.vjkit.mapper.JsonMapper;
import io.liuwei.autumn.converter.PageConverter;
import io.liuwei.autumn.enums.AccessLevelEnum;
import io.liuwei.autumn.enums.SourceFormatEnum;
import io.liuwei.autumn.model.Article;
import io.liuwei.autumn.model.ArticleHtml;
import io.liuwei.autumn.model.ArticleVO;
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
    private PageConverter pageConverter;

    // file path -> File. file path 以 "/" 开头，"/" 表示数据目录
    private volatile Map<String, File> allFileMap;

    private volatile Map<String, Article> articleMap;

    @PostConstruct
    public void init() throws IOException {
        reload();
    }

    @Caching(evict = {@CacheEvict(value = CacheConstants.ARTICLES)})
    public synchronized void reload() throws IOException {
        Map<String, File> allFileMap = dataFileDao.getAllFileMap();

        Map<String, Article> articleMap = Maps.newHashMapWithExpectedSize(allFileMap.size());
        for (Map.Entry<String, File> fileEntry : allFileMap.entrySet()) {
            if (SourceFormatEnum.getByFileName(fileEntry.getKey()) == SourceFormatEnum.ASCIIDOC) {
                String articlePath = StringUtils.substringBeforeLast(fileEntry.getKey(), ".");
                Article article = toArticle(fileEntry.getValue(), articlePath);
                articleMap.put(articlePath, article);
            }
        }
        log.info("found {} article", articleMap.size());
        this.allFileMap = allFileMap;
        this.articleMap = articleMap;
    }


    public File getFileByPath(String path) {
        return allFileMap.get(path);
    }

    public Article getArticleByPath(String path) {
        return articleMap.get(path);
    }

    @Cacheable(value = CacheConstants.ARTICLES)
    public List<Article> listArticles(AccessLevelEnum accessLevel) {
        if (accessLevel == AccessLevelEnum.PRIVATE) {
            return new ArrayList<>(articleMap.values());
        }
        return articleMap
                .entrySet()
                .stream()
                .map(Map.Entry::getValue)
                .filter(o -> o.getAccessLevel().getLevel() >= accessLevel.getLevel())
                .collect(Collectors.toList());
    }

    @Cacheable(value = CacheConstants.TREE_JSON)
    public String getTreeJson(AccessLevelEnum accessLevel) {
        return jsonMapper.toJson(TreeUtil.toArticleTree(listArticles(accessLevel)));
    }

    @Cacheable(value = CacheConstants.ARTICLE_VO, key = "#article.path")
    public ArticleVO toVO(Article article) {
        ArticleHtml articleHtml = pageConverter.convert(article.getTitle(), article.getContent(), article.getPath());

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

    private Article toArticle(File file, String articlePath) {
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

        Article article = asciidocArticleParser.parse(fileContent, articlePath);
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
