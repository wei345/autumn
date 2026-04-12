package io.liuwei.autumn.converter;

import io.liuwei.autumn.enums.SourceFormatEnum;
import io.liuwei.autumn.model.Article;
import io.liuwei.autumn.model.ArticleHtml;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @author liuwei
 * @since 2026-04-04 17:26
 */
@Component
@RequiredArgsConstructor
public class DocumentToHtmlConverterFacade {

    private final List<DocumentToHtmlConverter> converters;

    private DocumentToHtmlConverter getConverter(SourceFormatEnum format) {
        for (DocumentToHtmlConverter converter : converters)
            if (converter.supports(format))
                return converter;
        throw new RuntimeException("No document-to-html converts found for "
                + format);
    }

    public Article parseArticle(String text, String path, SourceFormatEnum format) {
        return getConverter(format).parseArticle(text, path, format);
    }

    public ArticleHtml toHtml(Article article) {
        return getConverter(article.getSourceFormat()).toHtml(article);
    }
}
