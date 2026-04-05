package io.liuwei.autumn.util;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * @author liuwei
 * @since 2026-04-05 13:52
 */
class LineReaderTest {

    String text1 = """
            = This is a title in a Asciidoc file
            :created: 2026-02-23 13:12:33 +1300
            
            :modified: 2026-03-28 23:36:02 +1300
            :category:
            :tags:
            :access: owner
            
            This is body.
            
            """;

    String meta1 = """
            :created: 2026-02-23 13:12:33 +1300
            
            :modified: 2026-03-28 23:36:02 +1300
            :category:
            :tags:
            :access: owner
            """;

    String body1 = """
            This is body.
            
            """;


    String text2 = """
            
            # This is a title in a Markdown file
            ---
            created: 2026-03-27 08:18:35 +1300
            modified: 2026-03-27 08:18:35 +1300
            category:
            tags:
            access: owner
            ---
            
            ## Section title
            
            Section body
            
            ## Another section
            
            balabala...
            """;


    String meta2 = """
            created: 2026-03-27 08:18:35 +1300
            modified: 2026-03-27 08:18:35 +1300
            category:
            tags:
            access: owner
            """;

    String body2 = """
            ## Section title
            
            Section body
            
            ## Another section
            
            balabala...
            
            
            """;

    @Test
    void testTextBlock() {
        assertThat(text1.startsWith("\n")).isFalse();
        assertThat(text1.endsWith("\n")).isTrue();

        assertThat(text2.startsWith("\n")).isTrue();
    }

    @Test
    void nextLinesAsString() {
        LineReader r1 = new LineReader(text1);

        assertThat(r1.nextNonBlankLine().trim()).isEqualTo("= This is a title in a Asciidoc file");

        assertThat(r1.nextLinesAsString(line -> StringUtils.isBlank(line) || line.startsWith(":")).trim())
                .isEqualTo(meta1.trim());

        assertThat(r1.remainingText().trim()).isEqualTo(body1.trim());
    }

    @Test
    void nextLinesAsStringUntil() {
        LineReader r2 = new LineReader(text2);

        assertThat(r2.nextNonBlankLine().trim()).isEqualTo("# This is a title in a Markdown file");

        assertThat(r2.nextNonBlankLine().trim()).isEqualTo("---");

        assertThat(r2.nextLinesAsStringUntil(line -> line.startsWith("---") && line.trim().equals("---")).trim())
                .isEqualTo(meta2.trim());

        assertThat(r2.nextLine().trim()).isEqualTo("---");

        assertThat(r2.remainingText().trim()).isEqualTo(body2.trim());
    }
}