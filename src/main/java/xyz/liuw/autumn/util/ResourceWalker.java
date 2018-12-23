package xyz.liuw.autumn.util;

import com.vip.vjtools.vjkit.io.IOUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collections;

import static java.nio.file.FileVisitResult.CONTINUE;

public class ResourceWalker {
    public static final String SPRING_BOOT_CLASSES = "/BOOT-INF/classes";
    private static Logger logger = LoggerFactory.getLogger(ResourceWalker.class);

    public static void walk(String relativePath, FileVisitor<? super Path> visitor) {
        try {
            URI uri = ResourceWalker.class.getResource(relativePath).toURI();

            if (uri.getScheme().equals("jar")) {
                walkJar(uri, relativePath, visitor);
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

    private static void walkJar(URI uri, String relativePath, FileVisitor<? super Path> visitor) {
        FileSystem fileSystem = null;
        try {
            fileSystem = FileSystems.newFileSystem(uri, Collections.emptyMap());

            Path path = fileSystem.getPath(relativePath);
            if (!Files.exists(path)) {
                path = fileSystem.getPath(SPRING_BOOT_CLASSES + relativePath);
            }

            if (!Files.exists(path)) {
                throw new RuntimeException(relativePath + " not exist");
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