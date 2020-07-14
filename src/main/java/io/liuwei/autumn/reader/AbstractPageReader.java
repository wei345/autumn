package io.liuwei.autumn.reader;

import com.vip.vjtools.vjkit.io.FileUtil;
import io.liuwei.autumn.domain.Page;
import lombok.Getter;
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
import java.util.Iterator;
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

    private final Pattern BLOG_FILE_NAME_PATTERN = Pattern.compile("^(\\d{4}-\\d{2}-\\d{2})-(.+)$");

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

        Page page = toPage(text, path, file.lastModified());
        page.setFile(file);
        return page;
    }

    @Override
    public Page toPage(@NotNull String text, @NotNull String path, long fileLastModified) {
        Lines lines = new Lines(text);
        Page page = new Page();

        page.setPath(path);
        page.setName(StringUtils.substringAfterLast(path, "/"));
        page.setArchived(path.startsWith(ARCHIVE_PATH_PREFIX));
        parseBlogPath(page);

        page.setSource(text);
        page.setSourceFormat(getSourceFormat());
        readHeader(lines, page);
        page.setTitle(readTitle(lines));
        page.setBody(lines.remainingText());

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

    protected abstract void readHeader(Lines lines, Page page);

    protected abstract String getTitlePrefix();

    protected abstract Page.SourceFormat getSourceFormat();

    protected String readTitle(Lines lines) {
        for(String line : lines){
            if (StringUtils.isBlank(line)) {
                continue;
            }

            String titlePrefix = getTitlePrefix();
            if (line.startsWith(titlePrefix)) {
                return line.substring(titlePrefix.length()).trim();
            } else {
                lines.back();
                return "";
            }
        }
        return "";
    }

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

    protected static class Lines implements Iterable<String> {
        @Getter
        private final String text;
        private int start; // default 0
        private int prev;

        Lines(String text) {
            this.text = text;
        }

        private String readLine() {
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

        @Override
        public Iterator<String> iterator() {
            return new Iterator<String>() {
                @Override
                public boolean hasNext() {
                    return start < text.length();
                }

                @Override
                public String next() {
                    return readLine();
                }
            };
        }
    }
}
