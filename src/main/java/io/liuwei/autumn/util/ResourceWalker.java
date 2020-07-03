package io.liuwei.autumn.util;

import com.vip.vjtools.vjkit.io.IOUtil;

import java.io.IOException;
import java.net.URI;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collections;

import static java.nio.file.FileVisitResult.*;

public class ResourceWalker {
    public static final String SPRING_BOOT_CLASSES = "/BOOT-INF/classes";

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

            if (!uri.getScheme().equals("file")) {
                throw new RuntimeException("Unsupported scheme " + uri.toString());
            }

            Path path = Paths.get(uri);
            Files.walkFileTree(path, visitor);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static void walkJar(URI uri, String classpath, FileVisitor<? super Path> visitor) {
        FileSystem fileSystem = null;
        try {
            fileSystem = FileSystems.newFileSystem(uri, Collections.emptyMap());

            Path path = fileSystem.getPath(classpath);
            if (!Files.exists(path)) {
                path = fileSystem.getPath(SPRING_BOOT_CLASSES + classpath);
            }

            if (!Files.exists(path)) {
                throw new RuntimeException(classpath + " not exist");
            }

            Files.walkFileTree(path, visitor);
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            if (fileSystem != null) {
                IOUtil.closeQuietly(fileSystem);
            }
        }
    }

    public static boolean isHidden(Path file) {
        return file.getFileName().toString().startsWith(".");
    }

    public static abstract class SkipHiddenFileVisitor extends SimpleFileVisitor<Path> {

        @Override
        public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
            if (isHidden(dir)) {
                return FileVisitResult.SKIP_SUBTREE;
            }
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
            if (isHidden(file)) {
                return CONTINUE;
            }

            return visitNonHiddenFile(file, attrs);
        }

        abstract public FileVisitResult visitNonHiddenFile(Path file, BasicFileAttributes attrs);
    }
}