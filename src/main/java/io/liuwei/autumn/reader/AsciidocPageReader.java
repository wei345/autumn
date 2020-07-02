package io.liuwei.autumn.reader;

import io.liuwei.autumn.domain.Page;
import org.apache.commons.lang3.StringUtils;

/**
 * @author liuwei
 * @since 2020-07-02 10:12
 */
public class AsciidocPageReader extends AbstractPageReader {

    @Override
    protected void readHeader(Lines lines, Page page) {
        for (String line : lines) {
            if (StringUtils.isNotBlank(line) && !line.startsWith(":")) {
                lines.back();
                break;
            }
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
