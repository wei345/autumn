package io.liuwei.autumn.util;

import com.vip.vjtools.vjkit.io.FileUtil;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

/**
 * @author liuwei
 * Created by liuwei on 2018/12/29.
 */
public class CommandJsCompressorTest {

    private final CommandJsCompressor commandJsCompressor = createJsCssCompressor();

    @Test
    public void testCompressJs() throws IOException {
        String js = FileUtil.toString(new File("src/main/resources/static/js/quick_search.js"));
        System.out.println(commandJsCompressor.compressJs(js));
    }

    private CommandJsCompressor createJsCssCompressor() {
        String jarPath = System.getProperty("user.home") + "/.m2/repository/com/google/javascript/closure-compiler/v20181210/closure-compiler-v20181210.jar";
        CommandJsCompressor compressor = new CommandJsCompressor(jarPath);
        compressor.setEnabled(true);
        return compressor;
    }
}