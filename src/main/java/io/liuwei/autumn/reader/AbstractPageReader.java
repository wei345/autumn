package io.liuwei.autumn.reader;

import com.vip.vjtools.vjkit.io.FileUtil;
import io.liuwei.autumn.domain.Page;
import io.liuwei.autumn.enums.SourceFormatEnum;
import io.liuwei.autumn.util.LineReader;
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
        LineReader lineReader = new LineReader(text);
        Page page = new Page();

        page.setPath(path);
        page.setName(StringUtils.substringAfterLast(path, "/"));
        page.setArchived(path.startsWith(ARCHIVE_PATH_PREFIX));
        parseBlogPath(page);

        page.setSource(text);
        page.setSourceFormat(getSourceFormat());
        readHeader(lineReader, page);
        page.setTitle(readTitle(lineReader));
        page.setBody(lineReader.remainingText());

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

    protected abstract void readHeader(LineReader lineReader, Page page);

    protected abstract String getTitlePrefix();

    protected abstract SourceFormatEnum getSourceFormat();

    protected String readTitle(LineReader lineReader) {
        for(String line : lineReader){
            if (StringUtils.isBlank(line)) {
                continue;
            }

            String titlePrefix = getTitlePrefix();
            if (line.startsWith(titlePrefix)) {
                return line.substring(titlePrefix.length()).trim();
            } else {
                lineReader.back();
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

}
