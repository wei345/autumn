package io.liuwei.autumn.util;

import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * @author liuwei
 * @since 2021-07-11 15:42
 */
public class IOUtil {

    public static byte[] toByteArray(File file) {
        try (InputStream in = new FileInputStream(file)) {
            return IOUtils.toByteArray(in);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static byte[] toByteArray(Path path) {
        try {
            return Files.readAllBytes(path);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static byte[] resourceToByteArray(String path) {
        try {
            return IOUtils.resourceToByteArray(path);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static String resourceToString(String path) {
        try {
            return IOUtils.resourceToString(path, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
