package io.liuwei.autumn.data;


import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.FastDateFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.ParseException;
import java.util.*;

/**
 * @author liuwei
 * Created by liuwei on 2018/11/12.
 */
public class MarkdownPageReader extends AbstractPageReader {

    static final String HEADER_BOUNDARY = "---";
    static final String HEADER_CREATED = "created:";
    static final String HEADER_MODIFIED = "modified:";
    static final String HEADER_CATEGORY = "category:";
    static final String HEADER_TAGS = "tags:";
    static final String HEADER_PUBLISHED = "published:";
    static final String TITLE_START_WITH = "# ";
    static final FastDateFormat DATE_PARSER_ON_SECOND = FastDateFormat.getInstance("yyyy-MM-dd HH:mm:ss");
    private static final FastDateFormat DATE_PARSER_ON_MINUTE = FastDateFormat.getInstance("yyyy-MM-dd HH:mm");
    private static final Logger logger = LoggerFactory.getLogger(MarkdownPageReader.class);

    @Override
    protected void readHeader(Lines lines, Page page) {
        boolean entered = false;
        for (String line : lines) {
            if (StringUtils.isBlank(line)) {
                continue;
            }

            if (!entered) {
                if (line.startsWith(HEADER_BOUNDARY)) {
                    // header 开始
                    entered = true;
                } else {
                    // 没有 header
                    lines.back();
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
                    if (value.length() > 0) {
                        Set<String> tags = new HashSet<>(Arrays.asList(value.split("\\s+")));
                        page.setTags(tags);
                    } else {
                        page.setTags(Collections.emptySet());
                    }
                } else if (line.startsWith(HEADER_PUBLISHED)) {
                    String value = line.substring(HEADER_PUBLISHED.length()).trim();
                    page.setPublished(Boolean.parseBoolean(value));
                } else if (line.startsWith(HEADER_BOUNDARY)) {
                    // header 结束
                    break;
                } else {
                    logger.warn("Unknown header: {}", line);
                }
            }
        }
    }

    @Override
    protected String getTitlePrefix() {
        return TITLE_START_WITH;
    }

    @Override
    protected Page.SourceFormat getSourceFormat() {
        return Page.SourceFormat.MARKDOWN;
    }

    private Date parseDate(String source) {
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


}
