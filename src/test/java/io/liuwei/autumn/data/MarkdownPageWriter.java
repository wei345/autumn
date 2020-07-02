package io.liuwei.autumn.data;

import com.vip.vjtools.vjkit.io.FileUtil;
import com.vip.vjtools.vjkit.text.StringBuilderHolder;
import io.liuwei.autumn.domain.Page;
import io.liuwei.autumn.reader.MarkdownPageReader;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;

import java.io.File;
import java.io.IOException;

/**
 * @author liuwei
 * Created by liuwei on 2018/12/18.
 */
class MarkdownPageWriter {

    private static final Logger logger = LoggerFactory.getLogger(MarkdownPageWriter.class);
    private static final String NEW_LINE = "\n";
    private static final StringBuilderHolder stringBuilderHolder = new StringBuilderHolder(1024);

    static void write(Page page, File file) {

        StringBuilder stringBuilder = stringBuilderHolder.get();

        // created, modified
        stringBuilder.append(MarkdownPageReader.HEADER_BOUNDARY).append(NEW_LINE)
                .append(MarkdownPageReader.HEADER_CREATED).append(" ").append(MarkdownPageReader.DATE_PARSER_ON_SECOND.format(page.getCreated())).append(NEW_LINE)
                .append(MarkdownPageReader.HEADER_MODIFIED).append(" ").append(MarkdownPageReader.DATE_PARSER_ON_SECOND.format(page.getModified())).append(NEW_LINE);

        // category
        stringBuilder.append(MarkdownPageReader.HEADER_CATEGORY);
        if (StringUtils.isNotBlank(page.getCategory())) {
            stringBuilder.append(" ").append(page.getCategory());
        }
        stringBuilder.append(NEW_LINE);

        // tags
        stringBuilder.append(MarkdownPageReader.HEADER_TAGS);
        if (!CollectionUtils.isEmpty(page.getTags())) {
            stringBuilder.append(" ").append(StringUtils.join(page.getTags(), " "));
        }
        stringBuilder.append(NEW_LINE);

        // published
        stringBuilder.append(MarkdownPageReader.HEADER_PUBLISHED).append(" ").append(page.isPublished()).append(NEW_LINE);

        // end front matter
        stringBuilder.append(MarkdownPageReader.HEADER_BOUNDARY).append(NEW_LINE).append(NEW_LINE);

        // title
        if (StringUtils.isNotBlank(page.getTitle())) {
            stringBuilder.append(MarkdownPageReader.TITLE_START_WITH).append(page.getTitle()).append(NEW_LINE).append(NEW_LINE);
        }

        // body
        stringBuilder.append(page.getBody().trim()).append(NEW_LINE);

        String text = stringBuilder.toString();
        if (!page.getSource().equals(text)) {
            logger.info("Writing {}", file.getAbsolutePath());
            try {
                FileUtil.write(text, file);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
