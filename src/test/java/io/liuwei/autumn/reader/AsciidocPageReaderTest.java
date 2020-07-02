package io.liuwei.autumn.reader;

import io.liuwei.autumn.domain.Page;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.commons.lang3.time.FastDateFormat;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Date;
import java.util.TreeSet;

import static org.assertj.core.api.Assertions.*;


/**
 * @author liuwei
 * @since 2020-07-02 14:01
 */
public class AsciidocPageReaderTest {

    @Test
    public void toPage() throws IOException {
        AsciidocPageReader reader = new AsciidocPageReader();

        String text = IOUtils.resourceToString("/example.adoc", StandardCharsets.UTF_8);

        Page page = reader.toPage(text, "/example.adoc", System.currentTimeMillis());

        assertThat(formatDate(page.getCreated())).isEqualTo("2020-07-02 13:34:12");
        assertThat(formatDate(page.getModified())).isEqualTo("2020-07-02 13:52:47");
        assertThat(page.isPublished()).isFalse();
        assertThat(page.getCategory()).isEqualTo("c1");
        assertThat(page.getTags()).isEqualTo(new TreeSet<>(Arrays.asList("tag1", "tag2")));
    }

    private String formatDate(Date date) {
        return DateFormatUtils.format(date, "yyyy-MM-dd HH:mm:ss");
    }

    @Test
    public void parse() throws ParseException {
        Date date = FastDateFormat.getInstance("yyyy-MM-dd HH:mm:ss").parse("2020-07-02 13:34:12 +0800");
        assertThat(formatDate(date)).isEqualTo("2020-07-02 13:34:12");
    }
}