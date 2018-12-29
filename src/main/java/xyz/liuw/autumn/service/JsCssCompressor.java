package xyz.liuw.autumn.service;

import com.google.common.annotations.VisibleForTesting;
import com.vip.vjtools.vjkit.io.FileUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import xyz.liuw.autumn.util.CommandExecutor;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * @author liuwei
 * Created by liuwei on 2018/12/29.
 */
@SuppressWarnings("SameParameterValue")
@Component
public class JsCssCompressor {
    private static final String JAR_FILE_PREFIX = "jar:file:";
    private static Logger logger = LoggerFactory.getLogger(JsCssCompressor.class);

    @Value("${autumn.yui-compressor-jar-full-path}")
    private String yuiJarFullPath;

    @Value("${autumn.closure-compiler-jar-full-path}")
    private String closureJarFullPath;

    @Value("${autumn.compressor.javascript.enabled}")
    private boolean compressJs;

    @Value("${autumn.compressor.css.enabled}")
    private boolean compressCss;

    private static String getJarFullPath(String classpath) {
        URL url = JsCssCompressor.class.getResource(classpath);
        if (url == null) {
            return null;
        }
        // e.g. jar:file:/path/to/.m2/repository/com/yahoo/platform/yui/yuicompressor/2.4.8/yuicompressor-2.4.8.jar!/com/yahoo/platform/yui/compressor/YUICompressor.class
        String path = url.toString();
        Validate.isTrue(path.startsWith(JAR_FILE_PREFIX));
        return path.substring(JAR_FILE_PREFIX.length(), path.length() - classpath.length() - 1);
    }

    /**
     * 压缩 js 或 css。
     * <p>
     * docs: http://yui.github.io/yuicompressor/
     *
     * @param type      js or css
     * @param input     待压缩的内容
     * @param lineBreak split long lines after a specific column, Specify 0 to get a line break
     *                  after each semi-colon in JavaScript, and after each rule in CSS.
     * @return 压缩后的内容
     */
    private static String yuiCompress(String type, String input, Integer lineBreak, String jar) {
        if (jar == null) {
            logger.warn("YUI jar not found, return raw input");
            return input;
        }

        List<String> commands = new ArrayList<>(10);
        commands.add("java");
        commands.add("-jar");
        commands.add(jar);
        commands.add("--type");
        commands.add(type);
        if (lineBreak != null) {
            commands.add("--line-break");
            commands.add(String.valueOf(lineBreak));
        }
        commands.add("--charset");
        commands.add("utf-8");

        CommandExecutor.Result result = CommandExecutor.execute(commands.toArray(new String[0]), input);
        if (result.hasError()) {
            logger.warn("YUI stderr: {}", result.getStderr());
            if (StringUtils.isBlank(result.getStdout())) {
                logger.warn("YUI stdout is blank, return raw input");
                return input;
            }
        }
        return result.getStdout();
    }

    // https://developers.google.com/closure/compiler/docs/gettingstarted_app
    @SuppressWarnings("ConstantConditions")
    @VisibleForTesting
    private static String closureCompress(String input, String jar) throws IOException {
        if (jar == null) {
            logger.warn("Closure Compiler jar not found, return raw input");
            return input;
        }

        boolean outToFile = false;
        Path inputJs, outputJs = null;
        inputJs = Files.createTempFile("autumn-", ".js");
        if (outToFile) {
            outputJs = Files.createTempFile("autumn-", ".js");
        }

        FileUtil.write(input, inputJs.toFile());

        // 有很多参数，见 closure-compiler.jar --help
        List<String> commands = new ArrayList<>(10);
        commands.add("java");
        commands.add("-jar");
        commands.add(jar);
        commands.add("--js");
        commands.add(inputJs.toString());
        //commands.add("--formatting");
        //commands.add("PRETTY_PRINT");
        if (outToFile) {
            commands.add("--js_output_file");
            commands.add(outputJs.toString());
        }

        logger.info("Closure Compiler compressing... {} chars", input.length());
        CommandExecutor.Result result = CommandExecutor.execute(commands.toArray(new String[0]));

        String resultJs;
        if (outToFile) {
            resultJs = FileUtil.toString(outputJs.toFile());
        } else {
            resultJs = result.getStdout();
        }
        if (resultJs == null) {
            resultJs = "";
        }

        Files.delete(inputJs);
        if (outToFile) {
            Files.delete(outputJs);
        }

        if (result.hasError()) {
            logger.warn("Closure Compiler stderr: {}", result.getStderr());
            if (StringUtils.isBlank(resultJs)) {
                logger.warn("Closure Compiler output js is blank, return raw input");
                return input;
            }
        }
        return resultJs;
    }

    @PostConstruct
    private void check() {
        if (yuiJarFullPath != null && !new File(yuiJarFullPath).exists()) {
            logger.warn("yui jar '{}' not exist", yuiJarFullPath);
            yuiJarFullPath = null;
        }
        if (closureJarFullPath != null && !new File(closureJarFullPath).exists()) {
            logger.warn("Closure Compiler jar '{}' not exist", closureJarFullPath);
            closureJarFullPath = null;
        }
    }

    public String compressJs(String input) {
        if (!compressJs) {
            return input;
        }
        try {
            return closureCompress(input, closureJarFullPath);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public String compressCss(String input) {
        if (!compressCss) {
            return input;
        }
        return yuiCompress("css", input, 0, yuiJarFullPath);
    }

    @VisibleForTesting
    void setYuiJarFullPath(String yuiJarFullPath) {
        this.yuiJarFullPath = yuiJarFullPath;
    }

    @VisibleForTesting
    void setClosureJarFullPath(String closureJarFullPath) {
        this.closureJarFullPath = closureJarFullPath;
    }

    @VisibleForTesting
    void setCompressJs(boolean compressJs) {
        this.compressJs = compressJs;
    }

    @VisibleForTesting
    void setCompressCss(boolean compressCss) {
        this.compressCss = compressCss;
    }
}
