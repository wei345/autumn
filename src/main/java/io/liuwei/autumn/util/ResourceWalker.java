package io.liuwei.autumn.util;

import io.liuwei.autumn.constant.Constants;

import java.io.File;
import java.net.URI;
import java.nio.file.*;
import java.util.Collections;

public class ResourceWalker {

    public static final String CLASSPATH_PREFIX = "classpath:";
    private static final String FILE_PREFIX = "file:";

    /**
     * @param url e.g. classpath:/static, file:src/main/resources/templates
     */
    public static void walk(String url, FileVisitor<? super Path> visitor) {
        try {

            if (url.startsWith(CLASSPATH_PREFIX)) {
                String path = url.substring(CLASSPATH_PREFIX.length());

                URI uri = ResourceWalker.class.getResource(path).toURI();

                if (uri.getScheme().equals("jar")) {
                    walkJar(uri, path, visitor);
                    return;
                }

                if (uri.getScheme().equals("file")) {
                    Files.walkFileTree(Paths.get(uri), visitor);
                    return;
                }
            }

            if (url.startsWith(FILE_PREFIX)) {
                String path = url.substring(FILE_PREFIX.length());
                Files.walkFileTree(new File(path).toPath(), visitor);
                return;
            }

            throw new IllegalArgumentException("Unsupported scheme. url=" + url);

        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static void walkJar(URI uri, String classpath, FileVisitor<? super Path> visitor) {
        try (FileSystem fileSystem = FileSystems.newFileSystem(uri, Collections.emptyMap())) {

            Path path = fileSystem.getPath(classpath);

            if (!Files.exists(path)) {
                path = fileSystem.getPath(Constants.BOOT_INF_CLASSES + classpath);
            }

            if (!Files.exists(path)) {
                throw new RuntimeException(classpath + " not exist");
            }

            Files.walkFileTree(path, visitor);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}