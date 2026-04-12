package io.liuwei.autumn.converter;

import io.liuwei.autumn.component.MediaRevisionResolver;
import io.liuwei.autumn.enums.AccessLevelEnum;
import io.liuwei.autumn.enums.SourceFormatEnum;
import io.liuwei.autumn.model.Article;
import io.liuwei.autumn.model.ArticleHtml;
import io.liuwei.autumn.util.LineReader;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.FastDateFormat;
import org.apache.commons.text.StringEscapeUtils;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.util.DigestUtils;

import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.util.*;

/**
 * @author liuwei
 * @since 2026-04-04 16:59
 */
public abstract class AbstractDocumentToHtmlConverter implements DocumentToHtmlConverter {

    protected static final FastDateFormat DATE_PARSER_ON_SECOND =
            FastDateFormat.getInstance("yyyy-MM-dd HH:mm:ss");
    protected static final String ATTR_CREATED = "created";
    protected static final String ATTR_MODIFIED = "modified";
    protected static final String ATTR_CATEGORY = "category";
    protected static final String ATTR_TAGS = "tags";
    protected static final String ATTR_ACCESS = "access";

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
            throw new RuntimeException("Error parsing date. dateString=" + dateString, e);
        }
    }

    protected String parseTitle(LineReader lineReader, String prefix) {
        String title = "";
        String line = lineReader.nextNonBlankLine();
        if (line != null) {
            if (line.startsWith(prefix))
                title = line.substring(prefix.length()).trim();
            else
                lineReader.back(); // Did not consume, return back
        }
        return title;
    }

    /**
     * Set created, modified, category, tags, access.
     */
    protected void setAttributes(Map<String, Object> attributes, Article article) {
        if (attributes == null)
            attributes = Collections.emptyMap();

        if (attributes.get(ATTR_CREATED) != null)
            article.setCreated(parseDate((String) attributes.get(ATTR_CREATED)));

        if (attributes.get(ATTR_MODIFIED) != null)
            article.setModified(parseDate((String) attributes.get(ATTR_MODIFIED)));

        String tagsString = StringUtils.trimToNull((String) attributes.get(ATTR_TAGS));
        if (tagsString != null)
            article.setTags(new LinkedHashSet<>(Arrays.asList(tagsString.split("\\s*,\\s*"))));
        else
            article.setTags(Collections.emptySet());
        article.setCategory(StringUtils.trimToNull((String) attributes.get(ATTR_CATEGORY)));
        article.setAccessLevel(AccessLevelEnum.of((String) attributes.get(ATTR_ACCESS), AccessLevelEnum.OWNER));
    }

    /**
     * Parse attributes, title, and body.
     */
    protected abstract void parseSource(Article article);

    @Override
    public ArticleHtml toHtml(Article article) {
        String title = article.getTitle();

        // Generate title HTML
        String titleId = "article-title";
        String titleHtml = "<h1 id=\"" + titleId + "\" class=\"heading\">" +
                StringEscapeUtils.escapeHtml4(title) +
                "<a class=\"anchor\" href=\"\"></a>" +
                "</h1>";

        Document bodyDoc = renderBodyAsDocument(article);

        String tocHtml = null;
        Element tocEl = bodyDoc.select("div.toc").first();
        if (tocEl != null) {
            // Remove TOC from the main content body
            tocEl.remove();

            // Insert Article Title at the top of the TOC list
            Element oldUl = tocEl.selectFirst("ul");
            if (oldUl != null) {
                Element articleTitleLink = new Element("a")
                        .attr("href", "#" + titleId)
                        .text(title);
                Element articleTitleLi = new Element("li")
                        .appendChild(articleTitleLink)
                        .appendChild(oldUl);
                Element newUl = new Element("ul")
                        .attr("class", "sectlevel0")
                        .appendChild(articleTitleLi);
                tocEl.appendChild(newUl);
            }
            tocHtml = tocEl.outerHtml();
        }

        scrollable(bodyDoc.select("table:not(.note > table)"));

        String bodyHtml = bodyDoc.html();
        return new ArticleHtml(title, titleHtml, tocHtml, bodyHtml);
    }

    protected abstract Document renderBodyAsDocument(Article article);

    public void scrollable(Elements elements) {
        // Wrap each item in elements in the div
        elements.wrap("<div class='scrollable'></div>");
    }
}
