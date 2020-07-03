package io.liuwei.autumn.util;

import org.apache.commons.io.IOUtils;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * @author liuwei
 * @since 2020-07-03 17:32
 */
public class JsCompressorTest {

    @Test
    public void compressJs() throws IOException {

        String source = IOUtils.resourceToString("/static/js/quick_search.js", StandardCharsets.UTF_8);
        String compressed = JsCompressor.compressJs("var not_change = {};", source);
        System.out.println(compressed);

    }
}