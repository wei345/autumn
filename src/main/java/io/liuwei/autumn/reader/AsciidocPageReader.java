package io.liuwei.autumn.reader;

import io.liuwei.autumn.domain.Page;
import io.liuwei.autumn.util.Asciidoctors;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.FastDateFormat;
import org.asciidoctor.ast.DocumentHeader;

import java.text.ParseException;
import java.util.Arrays;
import java.util.Date;
import java.util.Map;
import java.util.TreeSet;

/**
 * @author liuwei
 * @since 2020-07-02 10:12
 */
public class AsciidocPageReader extends AbstractPageReader {

    private static final String ATTR_CREATED = "created";
    private static final String ATTR_MODIFIED = "modified";
    private static final String ATTR_CATEGORY = "category";
    private static final String ATTR_TAGS = "tags";
    private static final String ATTR_PUBLISHED = "published";

    private static final FastDateFormat DATE_PARSER_ON_SECOND = FastDateFormat.getInstance("yyyy-MM-dd HH:mm:ss");

    @Override
    protected void readHeader(Lines lines, Page page) {
        DocumentHeader dh = Asciidoctors.getAsciidoctor().readDocumentHeader(lines.getText());

        Map<String, Object> attributes = dh.getAttributes();
        if (attributes.get(ATTR_CREATED) != null) {
            page.setCreated(parseDate((String) attributes.get(ATTR_CREATED)));
        }

        if (attributes.get(ATTR_MODIFIED) != null) {
            page.setModified(parseDate((String) attributes.get(ATTR_MODIFIED)));
        }

        if (attributes.get(ATTR_TAGS) != null) {
            String tagsString = (String) attributes.get(ATTR_TAGS);
            page.setTags(new TreeSet<>(Arrays.asList(tagsString.split("\\s*,\\s*"))));
        }

        page.setCategory((String) attributes.get(ATTR_CATEGORY));

        if (attributes.get(ATTR_PUBLISHED) != null) {
            page.setPublished(Boolean.parseBoolean((String) attributes.get(ATTR_PUBLISHED)));
        }

        for (String line : lines) {
            if (StringUtils.isNotBlank(line) && !line.startsWith(":")) {
                lines.back();
                break;
            }
        }
    }

    private Date parseDate(String dateString) {
        try {
            return DATE_PARSER_ON_SECOND.parse(dateString);
        } catch (ParseException e) {
            throw new RuntimeException("解析日期出错. dateString=" + dateString, e);
        }
    }

    @Override
    protected String getTitlePrefix() {
        return "= ";
    }

    @Override
    protected Page.SourceFormat getSourceFormat() {
        return Page.SourceFormat.ASCIIDOC;
    }
}
