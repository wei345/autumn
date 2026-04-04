package io.liuwei.autumn.parser;

import io.liuwei.autumn.component.MediaRevisionResolver;
import io.liuwei.autumn.enums.AccessLevelEnum;
import io.liuwei.autumn.enums.SourceFormatEnum;
import io.liuwei.autumn.model.Article;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.FastDateFormat;
import org.springframework.util.DigestUtils;

import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.util.*;

/**
 * @author liuwei
 * @since 2026-04-04 16:59
 */
public abstract class AbstractArticleParser implements ArticleParser {

    protected static final FastDateFormat DATE_PARSER_ON_SECOND = FastDateFormat.getInstance("yyyy-MM-dd HH:mm:ss");
    protected final String attrCreated = "created";
    protected final String attrModified = "modified";
    protected final String attrCategory = "category";
    protected final String attrTags = "tags";
    protected final String attrAccessLevel = "access";

    @Override
    public Article parseArticle(String text, String articlePath, SourceFormatEnum format) {
        if (text == null) return null;
        Article article = new Article();
        article.setSource(text);
        article.setSourceMd5(DigestUtils.md5DigestAsHex(text.getBytes(StandardCharsets.UTF_8)));
        article.setPath(articlePath);
        article.setSnapshotId(MediaRevisionResolver.getSnapshotId(article));
        article.setName(StringUtils.substringAfterLast(articlePath, "/"));
        article.setSourceFormat(format);

        parseSource(article);
        return article;
    }

    protected Date parseDate(String dateString) {
        try {
            return DATE_PARSER_ON_SECOND.parse(dateString);
        } catch (ParseException e) {
            throw new RuntimeException("解析日期出错. dateString=" + dateString, e);
        }
    }

    /**
     * Set created, modified, category, tags, access.
     */
    protected void setAttributes(Map<String, Object> attributes, Article article) {
        if (attributes == null)
            attributes = Collections.emptyMap();

        if (attributes.get(attrCreated) != null)
            article.setCreated(parseDate((String) attributes.get(attrCreated)));

        if (attributes.get(attrModified) != null)
            article.setModified(parseDate((String) attributes.get(attrModified)));

        String tagsString = StringUtils.trimToNull((String) attributes.get(attrTags));
        if (tagsString != null)
            article.setTags(new LinkedHashSet<>(Arrays.asList(tagsString.split("\\s*,\\s*"))));
        else
            article.setTags(Collections.emptySet());
        article.setCategory(StringUtils.trimToNull((String) attributes.get(attrCategory)));
        article.setAccessLevel(AccessLevelEnum.of((String) attributes.get(attrAccessLevel), AccessLevelEnum.OWNER));
    }

    /**
     * Parse attributes, title, and body.
     */
    protected abstract void parseSource(Article article);
}
