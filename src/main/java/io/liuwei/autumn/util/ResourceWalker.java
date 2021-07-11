package io.liuwei.autumn.util;

import io.liuwei.autumn.constant.Constants;

import java.net.URI;
import java.nio.file.*;
import java.util.Collections;

public class ResourceWalker {

    /**
     * @param classpath e.g. /static
     */
    public static void walk(String classpath, FileVisitor<? super Path> visitor) {
        try {
            URI uri = ResourceWalker.class.getResource(classpath).toURI();

            if (uri.getScheme().equals("jar")) {
                walkJar(uri, classpath, visitor);
                return;
            }

            if (uri.getScheme().equals("file")) {
                Files.walkFileTree(Paths.get(uri), visitor);
                return;
            }

            throw new RuntimeException("Unsupported scheme. uri=" + uri.toString());

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