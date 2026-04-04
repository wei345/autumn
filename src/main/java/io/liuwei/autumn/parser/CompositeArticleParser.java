package io.liuwei.autumn.parser;

import io.liuwei.autumn.model.Article;
import io.liuwei.autumn.model.ArticleHtml;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * @author liuwei
 * @since 2026-04-04 17:26
 */
@Component
@RequiredArgsConstructor
public class CompositeArticleParser extends AbstractArticleParser {

    private final AsciidocArticleParser asciidocArticleParser;
    private final MarkdownArticleParser markdownArticleParser;

    @Override
    protected void parseSource(Article article) {
        getParser(article).parseSource(article);
    }

    @Override
    public ArticleHtml toHtml(Article article) {
        return getParser(article).toHtml(article);
    }

    private AbstractArticleParser getParser(Article article) {
        return switch (article.getSourceFormat()) {
            case ASCIIDOC -> asciidocArticleParser;
            case MARKDOWN -> markdownArticleParser;
        };
    }
}
