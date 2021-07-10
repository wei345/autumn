package io.liuwei.autumn.util;

import com.google.common.annotations.VisibleForTesting;
import com.vip.vjtools.vjkit.io.FileUtil;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * 以命令行的方式调用 Closure Compiler jar 压缩 JS。
 *
 * @author liuwei
 * Created by liuwei on 2018/12/29.
 */
@Deprecated // Use JsCompressor
@SuppressWarnings("SameParameterValue")
public class CommandJsCompressor {
    private static final Logger logger = LoggerFactory.getLogger(CommandJsCompressor.class);

    private String closureJarFullPath;

    private boolean enabled;

    public CommandJsCompressor(String closureJarFullPath) {
        this.closureJarFullPath = closureJarFullPath;
    }

    // https://developers.google.com/closure/compiler/docs/gettingstarted_app
    @SuppressWarnings("ConstantConditions")
    private static String closureCompress(String input, String jar) throws IOException {
        if (jar == null) {
            logger.warn("Closure Compiler jar not found, return raw input");
            return input;
        }

        long startTime = System.currentTimeMillis();
        boolean outToFile = false;
        Path inputFile, outputFile = null;
        inputFile = Files.createTempFile("autumn-", ".js");
        if (outToFile) {
            outputFile = Files.createTempFile("autumn-", ".js");
        }

        FileUtil.write(input, inputFile.toFile());

        // 有很多参数，见 closure-compiler.jar --help
        List<String> commands = new ArrayList<>(10);
        commands.add("java");
        commands.add("-jar");
        commands.add(jar);
        commands.add("--rewrite_polyfills"); // 减少注入的 $jscomp 代码
        commands.add("false");
        commands.add("--js");
        commands.add(inputFile.toString());
        if (outToFile) {
            commands.add("--js_output_file");
            commands.add(outputFile.toString());
        }
        commands.add("--language_in");
        commands.add("ECMASCRIPT_2015"); // ES6
        commands.add("--language_out");
        commands.add("ECMASCRIPT_2015");

        logger.info("Closure Compiler compressing...");
        if (logger.isDebugEnabled()) {
            logger.debug(StringUtils.join(commands, ' '));
        }

        CommandExecutor.Result result = CommandExecutor.execute(commands.toArray(new String[0]));

        String resultJs = outToFile ? FileUtil.toString(outputFile.toFile()) : result.getStdout();
        if (resultJs == null) {
            resultJs = "";
        }

        Files.delete(inputFile);
        if (outToFile) {
            Files.delete(outputFile);
        }

        if (result.hasError()) {
            logger.warn("Closure Compiler stderr: {}", result.getStderr());
            if (StringUtils.isBlank(resultJs)) {
                logger.warn("Closure Compiler output js is blank, return raw input");
                return input;
            }
        }

        double percent = 100d * (input.length() - resultJs.length()) / input.length();
        logger.info("{}% less chars ({} -> {}), {} ms", (int) percent, input.length(), resultJs.length(),
                System.currentTimeMillis() - startTime);
        return resultJs;
    }

    @SuppressWarnings("WeakerAccess")
    public String compressJs(String input) {
        if (!enabled) {
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
        if (closureJarFullPath != null && !new File(closureJarFullPath).exists()) {
            logger.warn("Closure Compiler jar '{}' not exist", closureJarFullPath);
            closureJarFullPath = null;
        }
        this.closureJarFullPath = closureJarFullPath;
    }

    @VisibleForTesting
    void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

}
