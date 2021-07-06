package io.liuwei.autumn.util;

import org.junit.Test;

import static org.assertj.core.api.Assertions.*;


/**
 * @author liuwei
 * @since 2021-07-06 18:02
 */
public class KmpTest {

    @Test
    public void indexOfIgnoreCase() {
        String str = "abcd";
        String searchStr = "c";
        assertThat(Kmp.indexOfIgnoreCase(str, searchStr, 0)).isEqualTo(str.indexOf(searchStr));
        assertThat(Kmp.indexOfIgnoreCase(str, searchStr, 1)).isEqualTo(str.indexOf(searchStr));
        assertThat(Kmp.indexOfIgnoreCase(str, searchStr, 2)).isEqualTo(str.indexOf(searchStr));

        searchStr = "C";
        assertThat(Kmp.indexOfIgnoreCase(str, searchStr, 0)).isEqualTo(2);
        assertThat(Kmp.indexOfIgnoreCase(str, searchStr, 1)).isEqualTo(2);
        assertThat(Kmp.indexOfIgnoreCase(str, searchStr, 2)).isEqualTo(2);

        searchStr = "ab";
        assertThat(Kmp.indexOfIgnoreCase(str, searchStr, 0)).isEqualTo(str.indexOf(searchStr));

        searchStr = "ac";
        assertThat(Kmp.indexOfIgnoreCase(str, searchStr, 0)).isEqualTo(str.indexOf(searchStr));

        str = "aBCd";
        searchStr = "c";
        assertThat(Kmp.indexOfIgnoreCase(str, searchStr, 0)).isEqualTo(2);
    }
}