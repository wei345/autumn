package xyz.liuw.autumn.data;

import com.vip.vjtools.vjkit.io.FileUtil;
import com.vip.vjtools.vjkit.text.StringBuilderHolder;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;

import java.io.File;
import java.io.IOException;

import static xyz.liuw.autumn.data.PageParser.*;

/**
 * @author liuwei
 * Created by liuwei on 2018/12/18.
 */
class PageWriter {

    private static Logger logger = LoggerFactory.getLogger(PageWriter.class);
    private static final String NEW_LINE = "\n";
    private static StringBuilderHolder stringBuilderHolder = new StringBuilderHolder(1024);

    static void write(Page page, File file) {

        StringBuilder stringBuilder = stringBuilderHolder.get();

        // created, modified
        stringBuilder.append(HEADER_BOUNDARY).append(NEW_LINE)
                .append(HEADER_CREATED).append(" ").append(DATE_PARSER_ON_SECOND.format(page.getCreated())).append(NEW_LINE)
                .append(HEADER_MODIFIED).append(" ").append(DATE_PARSER_ON_SECOND.format(page.getModified())).append(NEW_LINE);

        // category
        stringBuilder.append(HEADER_CATEGORY);
        if (StringUtils.isNotBlank(page.getCategory())) {
            stringBuilder.append(" ").append(page.getCategory());
        }
        stringBuilder.append(NEW_LINE);

        // tags
        stringBuilder.append(HEADER_TAGS);
        if (!CollectionUtils.isEmpty(page.getTags())) {
            stringBuilder.append(" ").append(StringUtils.join(page.getTags(), " "));
        }
        stringBuilder.append(NEW_LINE);

        // published
        stringBuilder.append(HEADER_PUBLISHED).append(" ").append(page.isPublished()).append(NEW_LINE);

        // end front matter
        stringBuilder.append(HEADER_BOUNDARY).append(NEW_LINE).append(NEW_LINE);

        // title
        if (StringUtils.isNotBlank(page.getTitle())) {
            stringBuilder.append(TITLE_START_WITH).append(page.getTitle()).append(NEW_LINE).append(NEW_LINE);
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
