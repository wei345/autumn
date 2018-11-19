package xyz.liuw.autumn.data;

import org.apache.commons.lang3.time.DateFormatUtils;
import org.junit.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author liuwei
 * Created by liuwei on 2018/11/18.
 */
public class PageParserTest {

    @Test
    public void parse() throws IOException {
        String text = "---\n" +
                "created: 2018-02-20 12:59:50\n" +
                "modified: 2018-02-20 12:59:50\n" +
                "category: test\n" +
                "tags: java text\n" +
                "published: true\n" +
                "foo: 123\n" +
                "---\n" +
                "\n" +
                "# Misc\n" +
                "\n" +
                "## 探测文件类型\n" +
                "\n" +
                "[Tika](http://tika.apache.org/)\n" +
                "读取文件开头的一些字符，猜测文件类型。\n" +
                "要检查是否 UTF-8 文件，只依赖 tika-core 就可以。\n" +
                "如果是 Ascii 或 UTF-8，TextDetector.detect 会返回 TEXT_PLAIN，否则返回 OCTET_STREAM。\n" +
                "tika-parsers 依赖太多，有需要才用。";

        Page page = PageParser.parse(text);

        assertThat(page.getTitle()).isEqualTo("Misc");
        assertThat(DateFormatUtils.format(page.getCreated(), "yyyy-MM-dd HH:mm:ss")).isEqualTo("2018-02-20 12:59:50");
        assertThat(page.getTags().contains("java")).isTrue();
        assertThat(page.isPublished()).isTrue();
        System.out.println(page.getBody());
    }
}