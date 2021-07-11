package io.liuwei.autumn.manager;

import io.liuwei.autumn.enums.AccessLevelEnum;
import io.liuwei.autumn.model.Article;
import io.liuwei.autumn.util.LineReader;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.FastDateFormat;
import org.asciidoctor.Asciidoctor;
import org.asciidoctor.ast.DocumentHeader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.DigestUtils;

import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.util.*;

/**
 * @author liuwei
 * @since 2021-07-07 17:21
 */
@Component
public class AsciidocArticleParser {
    private static final FastDateFormat DATE_PARSER_ON_SECOND = FastDateFormat.getInstance("yyyy-MM-dd HH:mm:ss");
    private final String attrCreated = "created";
    private final String attrModified = "modified";
    private final String attrCategory = "category";
    private final String attrTags = "tags";
    private final String attrAccessLevel = "access";
    private final String titlePrefix = "= ";
    private final Asciidoctor asciidoctor;
    private final String ASCIIDOC_HEADER_PREFIX = ":";

    @Autowired
    public AsciidocArticleParser(Asciidoctor asciidoctor) {
        this.asciidoctor = asciidoctor;
    }

    public Article parse(String text, String path) {
        if (text == null) {
            return null;
        }
        Article article = new Article();
        LineReader lineReader = new LineReader(text);
        parseHeader(lineReader, article);
        article.setTitle(parseTitle(lineReader));
        article.setContent(parseContent(lineReader));
        article.setSource(text);
        article.setSourceMd5(DigestUtils.md5DigestAsHex(text.getBytes(StandardCharsets.UTF_8)));
        article.setPath(path);
        article.setName(StringUtils.substringAfterLast(path, "/"));
        return article;
    }

    private void parseHeader(LineReader lineReader, Article article) {
        DocumentHeader dh = asciidoctor.readDocumentHeader(lineReader.getText());

        Map<String, Object> attributes = dh.getAttributes();
        if (attributes.get(attrCreated) != null) {
            article.setCreated(parseDate((String) attributes.get(attrCreated)));
        }

        if (attributes.get(attrModified) != null) {
            article.setModified(parseDate((String) attributes.get(attrModified)));
        }

        String tagsString = StringUtils.trimToNull((String) attributes.get(attrTags));
        if (tagsString != null) {
            article.setTags(new LinkedHashSet<>(Arrays.asList(tagsString.split("\\s*,\\s*"))));
        } else {
            article.setTags(Collections.emptySet());
        }

        article.setCategory(StringUtils.trimToNull((String) attributes.get(attrCategory)));

        article.setAccessLevel(AccessLevelEnum.of((String) attributes.get(attrAccessLevel), AccessLevelEnum.OWNER));

        for (String line : lineReader) {
            if (StringUtils.isNotBlank(line) && !line.startsWith(":")) {
                lineReader.back();
                break;
            }
        }
    }

    private String parseTitle(LineReader lineReader) {
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

    private String parseContent(LineReader lineReader){
        for (String line : lineReader) {
            if(StringUtils.isNotBlank(line) && !line.startsWith(ASCIIDOC_HEADER_PREFIX)){
                lineReader.back();
                break;
            }
        }
        return lineReader.remainingText();
    }


    private Date parseDate(String dateString) {
        try {
            return DATE_PARSER_ON_SECOND.parse(dateString);
        } catch (ParseException e) {
            throw new RuntimeException("解析日期出错. dateString=" + dateString, e);
        }
    }

}
