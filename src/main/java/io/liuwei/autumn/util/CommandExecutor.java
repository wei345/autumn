package io.liuwei.autumn.util;

import com.google.common.io.ByteStreams;
import com.vip.vjtools.vjkit.text.StringBuilderHolder;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.Arrays;
import java.util.concurrent.*;

import static java.nio.charset.StandardCharsets.*;

/**
 * @author liuwei
 * Created by liuwei on 2018/12/29.
 */
@SuppressWarnings("WeakerAccess")
public class CommandExecutor {

    private static ThreadPoolExecutor executor;

    private static ThreadPoolExecutor newThreadPoolExecutor() {
        return new ThreadPoolExecutor(
                2,
                2,
                1,
                TimeUnit.MINUTES,
                new LinkedBlockingQueue<>(2),
                new ThreadFactory() {
                    int count = 0;

                    @Override
                    public Thread newThread(Runnable r) {
                        return new Thread(r, "command-thread-" + ++count);
                    }
                });
    }

    /**
     * 在一个独立的进程中执行命令。
     *
     * @param command 要执行的命令及参数，shell 中一些需要加单引号或双引号的地方这里不需要
     * @return Result 包含 stdout 和 stderr 输出的内容
     */
    public static Result execute(String[] command) {
        return execute(command, null);
    }

    /**
     * 在一个独立的进程中执行命令。
     *
     * @param command 要执行的命令及参数，shell 中一些需要加单引号或双引号的地方这里不需要
     * @param input   stdin, utf8 string
     * @return Result 包含 stdout 和 stderr 输出的内容
     */
    public synchronized static Result execute(String[] command, String input) {
        if (executor == null) {
            executor = newThreadPoolExecutor();
        }

        Process process = null;
        try {
            process = Runtime.getRuntime().exec(command);
            if (input != null && input.length() > 0) {
                OutputStream procStdin = process.getOutputStream();
                ByteStreams.copy(new ByteArrayInputStream(input.getBytes(UTF_8)), procStdin);
                procStdin.flush();
                procStdin.close();
            }

            CommandReader outReader = new CommandReader("stdout", process.getInputStream());
            CommandReader errReader = new CommandReader("stderr", process.getErrorStream());
            executor.execute(outReader);
            executor.execute(errReader);

            // 等待执行完成
            if (!process.waitFor(10, TimeUnit.MINUTES)) {
                throw new TimeoutException("command timeout: " + Arrays.toString(command) +
                        ", input: '" + input + "'");
            }

            return new Result(outReader.getContent(), errReader.getContent());
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            if (process != null && process.isAlive()) {
                process.destroyForcibly();
            }
        }
    }

    static class CommandReader implements Runnable {

        private static final Logger logger = LoggerFactory.getLogger(CommandReader.class);
        private static final StringBuilderHolder stringBuilderHolder = new StringBuilderHolder(1024);
        private final String name;
        private final InputStream in;
        private String content;

        CommandReader(String name, InputStream in) {
            this.name = name;
            this.in = in;
        }

        @Override
        public void run() {
            StringBuilder stringBuilder = stringBuilderHolder.get();
            try (BufferedReader br = new BufferedReader(new InputStreamReader(in, UTF_8))) {
                char[] buff = new char[1024];
                int len;
                while ((len = br.read(buff, 0, buff.length)) != -1) {
                    stringBuilder.append(buff, 0, len);
                }
            } catch (IOException e) {
                logger.warn("", e);
            }
            content = stringBuilder.toString();
        }

        public String getContent() {
            return content;
        }
    }

    @SuppressWarnings("WeakerAccess")
    @Getter
    public static class Result {
        private final String stdout;
        private final String stderr;
        private final boolean error;

        Result(String stdout, String stderr) {
            this.stdout = stdout;
            this.stderr = stderr;
            error = StringUtils.isNotEmpty(stderr);
        }
    }
}
