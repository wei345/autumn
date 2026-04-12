package io.liuwei.autumn.converter;

import io.liuwei.autumn.enums.SourceFormatEnum;
import io.liuwei.autumn.model.Article;
import io.liuwei.autumn.model.ArticleHtml;

/**
 * @author liuwei
 * @since 2026-04-04 16:59
 */
public interface DocumentToHtmlConverter {

    boolean supports(SourceFormatEnum format);

    Article parseArticle(String text, String path, SourceFormatEnum format);

    ArticleHtml toHtml(Article article);
}
