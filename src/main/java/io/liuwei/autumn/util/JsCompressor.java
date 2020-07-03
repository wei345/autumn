package io.liuwei.autumn.util;

import com.google.javascript.jscomp.CompilationLevel;
import com.google.javascript.jscomp.CompilerOptions;
import com.google.javascript.jscomp.JSError;
import com.google.javascript.jscomp.SourceFile;
import lombok.extern.slf4j.Slf4j;

import java.util.logging.Level;

/**
 * @author liuwei
 * @since 2020-07-03 16:59
 */
@Slf4j
public class JsCompressor {

    /**
     * @param extern source 依赖的 JS
     * @param source 要被压缩的 JS
     * @return 压缩后的 source
     */
    public static String compressJs(String extern, String source) {

        com.google.javascript.jscomp.Compiler.setLoggingLevel(Level.INFO);
        com.google.javascript.jscomp.Compiler compiler = new com.google.javascript.jscomp.Compiler();

        CompilerOptions options = new CompilerOptions();
        CompilationLevel.SIMPLE_OPTIMIZATIONS.setOptionsForCompilationLevel(options);
        options.setRewritePolyfills(false); // 减少注入的 $jscomp 代码
        options.setLanguageIn(CompilerOptions.LanguageMode.ECMASCRIPT_2015);
        options.setLanguageOut(CompilerOptions.LanguageMode.ECMASCRIPT_2015);

        // 文件名不必对应一个真实存在的文件，只是用于显示错误信息
        String fileName = System.currentTimeMillis() + ".js";
        SourceFile sourceFile = SourceFile.builder().buildFromCode(fileName, source);

        long start = System.currentTimeMillis();
        compiler.compile(SourceFile.fromCode("extern.js", extern), sourceFile, options);

        for (JSError message : compiler.getWarnings()) {
            log.warn(message.toString());
        }

        for (JSError message : compiler.getErrors()) {
            log.error(message.toString());
        }

        String result = compiler.toSource();

        double percent = 100d * (source.length() - result.length()) / source.length();
        log.info("{} ms. 减少了 {}% 字符 ({} -> {}).", System.currentTimeMillis() - start,
                (int) percent, source.length(), result.length());

        return result;
    }

}
