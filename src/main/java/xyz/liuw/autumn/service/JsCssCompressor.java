package xyz.liuw.autumn.service;

import com.google.common.annotations.VisibleForTesting;
import com.vip.vjtools.vjkit.io.FileUtil;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import xyz.liuw.autumn.util.CommandExecutor;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
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
    private static Logger logger = LoggerFactory.getLogger(JsCssCompressor.class);

    @Value("${autumn.closure-compiler-jar-full-path}")
    private String closureJarFullPath;

    @Value("${autumn.compressor.javascript.enabled}")
    private boolean compressJs;

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
        if (outToFile) {
            commands.add("--js_output_file");
            commands.add(outputJs.toString());
        }

        logger.info("Closure Compiler compressing... {} chars", input.length());
        CommandExecutor.Result result = CommandExecutor.execute(commands.toArray(new String[0]));

        String resultJs = outToFile ? FileUtil.toString(outputJs.toFile()) : result.getStdout();
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
        if (closureJarFullPath != null && !new File(closureJarFullPath).exists()) {
            logger.warn("Closure Compiler jar '{}' not exist", closureJarFullPath);
            closureJarFullPath = null;
        }
    }

    @SuppressWarnings("WeakerAccess")
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

    @VisibleForTesting
    void setClosureJarFullPath(String closureJarFullPath) {
        this.closureJarFullPath = closureJarFullPath;
    }

    @VisibleForTesting
    void setCompressJs(boolean compressJs) {
        this.compressJs = compressJs;
    }

}
