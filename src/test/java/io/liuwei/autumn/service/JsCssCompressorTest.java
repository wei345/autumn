package io.liuwei.autumn.service;

import com.vip.vjtools.vjkit.io.FileUtil;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

/**
 * @author liuwei
 * Created by liuwei on 2018/12/29.
 */
public class JsCssCompressorTest {

    private final JsCssCompressor jsCssCompressor = createJsCssCompressor();

    @Test
    public void testCompressJs() throws IOException {
        String js = FileUtil.toString(new File("src/main/resources/static/js/quick_search.js"));
        System.out.println(jsCssCompressor.compressJs(js));
    }

    private JsCssCompressor createJsCssCompressor() {
        JsCssCompressor compressor = new JsCssCompressor();
        compressor.setCompressJs(true);
        compressor.setClosureJarFullPath(System.getProperty("user.home") + "/.m2/repository/com/google/javascript/closure-compiler/v20181210/closure-compiler-v20181210.jar");
        return compressor;
    }
}