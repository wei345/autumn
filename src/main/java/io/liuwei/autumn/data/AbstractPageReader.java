package io.liuwei.autumn.data;

import com.vip.vjtools.vjkit.io.FileUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.FastDateFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.validation.constraints.NotNull;
import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.Collections;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author liuwei
 * @since 2020-07-01 17:34
 */
public abstract class AbstractPageReader implements PageReader {

    private final Logger logger = LoggerFactory.getLogger(AbstractPageReader.class);

    @SuppressWarnings("FieldCanBeLocal")
    private final String ARCHIVE_PATH_PREFIX = "/archive/";

    private final Pattern BLOG_FILE_NAME_PATTERN = Pattern.compile("^(\\d{4}-\\d{2}-\\d{2})-(.+)\\.md$");

    private final FastDateFormat DATE_PARSER_ON_DAY = FastDateFormat.getInstance("yyyy-MM-dd");

    @Override
    public Page toPage(File file, String path) {
        if (logger.isDebugEnabled()) {
            logger.debug("Parsing {}", file.getAbsolutePath());
        }

        String text;
        try {
            text = FileUtil.toString(file);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return toPage(text, path, file.lastModified());
    }

    @Override
    public Page toPage(@NotNull String text, @NotNull String path, long fileLastModified) {
        TextSource source = new TextSource(text);
        Page page = new Page();

        page.setPath(path);
        page.setName(StringUtils.substringAfterLast(path, "/"));
        page.setArchived(path.startsWith(ARCHIVE_PATH_PREFIX));
        parseBlogPath(page);

        page.setSource(text);
        read(source, page);

        page.setFileLastModified(fileLastModified);

        if (page.getCreated() == null) {
            page.setCreated(new Date(fileLastModified));
        }

        if (page.getModified() == null) {
            page.setModified(new Date(fileLastModified));
        }

        if (page.getTags() == null) {
            page.setTags(Collections.emptySet());
        }

        return page;
    }

    protected abstract void read(MarkdownPageReader.TextSource source, Page page);

    private void parseBlogPath(Page page) {
        Matcher matcher = BLOG_FILE_NAME_PATTERN.matcher(page.getName());
        if (!matcher.find()) {
            return;
        }
        String dateStr = matcher.group(1);
        try {
            page.setBlogDate(DATE_PARSER_ON_DAY.parse(dateStr));
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
        String blogName = matcher.group(2);
        String blogPath = "/" + dateStr.replace('-', '/') + "/" + blogName;
        page.setBlogPath(blogPath);
        page.setBlog(true);
    }

    protected static class TextSource {
        private final String text;
        private int start; // default 0
        private int prev;

        TextSource(String text) {
            this.text = text;
        }

        String readLine() {
            String text = this.text;
            int start = this.start;

            if (start >= text.length()) {
                return null;
            }

            int end = text.indexOf("\n", start);
            if (end == -1) {
                end = text.length();
            }

            String line = text.substring(start, end);

            this.prev = start;
            this.start = end + 1;
            return line;
        }

        void back() {
            this.start = this.prev;
        }

        String remainingText() {
            return text.substring(start);
        }
    }
}
