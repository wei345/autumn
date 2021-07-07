package io.liuwei.autumn;

import io.liuwei.autumn.enums.AccessLevelEnum;
import io.liuwei.autumn.model.Article;
import io.liuwei.autumn.util.Asciidoctors;
import io.liuwei.autumn.util.LineReader;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.FastDateFormat;
import org.asciidoctor.ast.DocumentHeader;
import org.springframework.util.DigestUtils;

import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.util.*;

/**
 * @author liuwei
 * @since 2021-07-07 17:21
 */
public class AsciidocArticleParser {
    private final String attrCreated = "created";
    private final String attrModified = "modified";
    private final String attrCategory = "category";
    private final String attrTags = "tags";
    private final String attrAccessLevel = "access";
    private final String titlePrefix = "= ";

    private static final FastDateFormat DATE_PARSER_ON_SECOND = FastDateFormat.getInstance("yyyy-MM-dd HH:mm:ss");

    public Article parse(String text, String path) {
        if (text == null) {
            return null;
        }
        Article article = new Article();
        LineReader lineReader = new LineReader(text);
        parseHeader(lineReader, article);
        article.setTitle(parseTitle(lineReader));
        article.setContent(lineReader.remainingText());
        article.setSource(text);
        article.setSourceMd5(DigestUtils.md5DigestAsHex(text.getBytes(StandardCharsets.UTF_8)));
        article.setPath(path);
        article.setName(StringUtils.substringAfterLast(path, "/"));
        return article;
    }

    protected void parseHeader(LineReader lineReader, Article article) {
        DocumentHeader dh = Asciidoctors.getAsciidoctor().readDocumentHeader(lineReader.getText());

        Map<String, Object> attributes = dh.getAttributes();
        if (attributes.get(attrCreated) != null) {
            article.setCreated(parseDate((String) attributes.get(attrCreated)));
        }

        if (attributes.get(attrModified) != null) {
            article.setModified(parseDate((String) attributes.get(attrModified)));
        }

        if (attributes.get(attrTags) != null) {
            String tagsString = (String) attributes.get(attrTags);
            article.setTags(new LinkedHashSet<>(Arrays.asList(tagsString.split("\\s*,\\s*"))));
        } else {
            article.setTags(Collections.emptySet());
        }

        article.setCategory((String) attributes.get(attrCategory));

        if (attributes.get(attrAccessLevel) != null) {
            AccessLevelEnum accessLevel = AccessLevelEnum
                    .of((String) attributes.get(attrAccessLevel), AccessLevelEnum.PRIVATE);
            article.setAccessLevel(accessLevel);
        }

        for (String line : lineReader) {
            if (StringUtils.isNotBlank(line) && !line.startsWith(":")) {
                lineReader.back();
                break;
            }
        }
    }

    protected String parseTitle(LineReader lineReader) {
        for (String line : lineReader) {
            if (StringUtils.isBlank(line)) {
                continue;
            }

            if (line.startsWith(titlePrefix)) {
                return line.substring(titlePrefix.length()).trim();
            } else {
                lineReader.back();
                return "";
            }
        }
        return "";
    }


    private Date parseDate(String dateString) {
        try {
            return DATE_PARSER_ON_SECOND.parse(dateString);
        } catch (ParseException e) {
            throw new RuntimeException("解析日期出错. dateString=" + dateString, e);
        }
    }

}
