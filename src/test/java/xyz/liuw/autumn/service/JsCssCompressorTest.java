package xyz.liuw.autumn.service;

import com.vip.vjtools.vjkit.io.FileUtil;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

/**
 * @author liuwei
 * Created by liuwei on 2018/12/29.
 */
public class JsCssCompressorTest {

    private JsCssCompressor jsCssCompressor = getJsCssCompressor();

    @Test
    public void testCompressJs() throws IOException {
        String js = FileUtil.toString(new File("src/main/resources/static/js/quick_search.js"));
        System.out.println(jsCssCompressor.compressJs(js));
    }

    @Test
    public void testCompressCss() {
        System.out.println(jsCssCompressor.compressCss("code {\n" +
                "    background: #f4f5f6;\n" +
                "    border-radius: 4px;\n" +
                "    font-size: 86%;\n" +
                "    margin: 0 2px;\n" +
                "    padding: 2px 5px;\n" +
                "    white-space: nowrap;\n" +
                "    word-break: normal;\n" +
                "    tab-size: 4;\n" +
                "}\n" +
                "\n" +
                "pre {\n" +
                "    background: #f4f5f6;\n" +
                "    overflow-y: hidden;\n" +
                "}\n" +
                "\n" +
                "pre > code {\n" +
                "    margin: 0;\n" +
                "    border-radius: 0;\n" +
                "    display: block;\n" +
                "    padding: 10px 16px;\n" +
                "    white-space: pre;\n" +
                "    overflow-y: hidden;\n" +
                "    -webkit-font-smoothing: auto;\n" +
                "}"));
    }

    private JsCssCompressor getJsCssCompressor() {
        JsCssCompressor compressor = new JsCssCompressor();
        compressor.setCompressCss(true);
        compressor.setCompressJs(true);
        compressor.setClosureJarFullPath("../../../../.m2/repository/com/google/javascript/closure-compiler/v20181210/closure-compiler-v20181210.jar");
        compressor.setYuiJarFullPath("../../../../.m2/repository/com/yahoo/platform/yui/yuicompressor/2.4.8/yuicompressor-2.4.8.jar");
        return compressor;
    }
}