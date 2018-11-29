package xyz.liuw.autumn.data;


import com.vip.vjtools.vjkit.io.FileUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateParser;
import org.apache.commons.lang3.time.FastDateFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

/**
 * @author liuwei
 * Created by liuwei on 2018/11/12.
 */
public class PageParser {

    private static final DateParser DATE_PARSER_ON_SECOND = FastDateFormat.getInstance("yyyy-MM-dd HH:mm:ss");
    private static final DateParser DATE_PARSER_ON_MINUTE = FastDateFormat.getInstance("yyyy-MM-dd HH:mm");
    private static final String HEADER_BOUNDARY = "---";
    private static final String HEADER_CREATED = "created:";
    private static final String HEADER_MODIFIED = "modified:";
    private static final String HEADER_CATEGORY = "category:";
    private static final String HEADER_TAGS = "tags:";
    private static final String HEADER_PUBLISHED = "published:";
    private static final String TITLE_START_WITH = "# ";
    private static Logger logger = LoggerFactory.getLogger(PageParser.class);

    public static Page parse(File file) {
        logger.info("Parsing {}", file.getAbsolutePath());
        String text;
        try {
            text = FileUtil.toString(file);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        Page page = parse(text);
        page.setLastModified(file.lastModified());
        return page;
    }

    public static Page parse(String text) {
        TextSource source = new TextSource(text);
        Page page = new Page();
        parseHeader(source, page);
        parseTitle(source, page);
        parseBody(source, page);
        page.setSource(text);
        return page;
    }

    private static void parseHeader(TextSource source, Page page) {
        boolean entered = false;
        String line;
        while ((line = source.readLine()) != null) {
            if (StringUtils.isBlank(line)) {
                continue;
            }

            if (!entered) {
                if (line.startsWith(HEADER_BOUNDARY)) {
                    // header 开始
                    entered = true;
                } else {
                    // 没有 header
                    source.back();
                    break;
                }
            } else {
                if (line.startsWith(HEADER_CREATED)) {
                    String value = line.substring(HEADER_CREATED.length()).trim();
                    page.setCreated(parseDate(value));
                } else if (line.startsWith(HEADER_MODIFIED)) {
                    String value = line.substring(HEADER_MODIFIED.length()).trim();
                    page.setModified(parseDate(value));
                } else if (line.startsWith(HEADER_CATEGORY)) {
                    String value = line.substring(HEADER_CATEGORY.length()).trim();
                    page.setCategory(value);
                } else if (line.startsWith(HEADER_TAGS)) {
                    String value = line.substring(HEADER_TAGS.length()).trim();
                    Set<String> tags = new HashSet<>(Arrays.asList(value.split("\\s+")));
                    page.setTags(tags);
                } else if (line.startsWith(HEADER_PUBLISHED)) {
                    String value = line.substring(HEADER_PUBLISHED.length()).trim();
                    page.setPublished(Boolean.valueOf(value));
                } else if (line.startsWith(HEADER_BOUNDARY)) {
                    // header 结束
                    break;
                } else {
                    logger.warn("Unknown header [{}]", line);
                }
            }
        }
    }

    private static Date parseDate(String source) {
        try {
            return DATE_PARSER_ON_SECOND.parse(source);
        } catch (ParseException e) {
            try {
                return DATE_PARSER_ON_MINUTE.parse(source);
            } catch (ParseException e1) {
                throw new RuntimeException("failed to parse " + source, e);
            }
        }
    }

    private static void parseTitle(TextSource source, Page page) {
        String line;
        while ((line = source.readLine()) != null) {
            if (StringUtils.isBlank(line)) {
                continue;
            }

            if (line.startsWith(TITLE_START_WITH)) {
                String value = line.substring(TITLE_START_WITH.length()).trim();
                page.setTitle(value);
            } else {
                source.back();
            }
            break;
        }
    }

    private static void parseBody(TextSource source, Page page) {
        String body = source.remainingText();
        page.setBody(body);
    }

    static class TextSource {
        private String text;
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
