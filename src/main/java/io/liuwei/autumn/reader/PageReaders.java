package io.liuwei.autumn.reader;

import org.apache.commons.lang3.StringUtils;

/**
 * @author liuwei
 * @since 2020-07-02 10:56
 */
public class PageReaders {

    private static final PageReader markdownPageReader = new MarkdownPageReader();

    private static final PageReader asciidocPageReader = new AsciidocPageReader();

    public static PageReader getPageReader(String filename) {

        if(StringUtils.endsWithIgnoreCase(filename, ".adoc")){
            return asciidocPageReader;
        }

        if(StringUtils.endsWithIgnoreCase(filename, ".md")){
            return markdownPageReader;
        }

        throw new RuntimeException("Unsupported file type: " + filename);

    }

}
