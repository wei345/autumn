package xyz.liuw.autumn.util;

import com.vip.vjtools.vjkit.io.IOUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.nio.file.*;
import java.util.Collections;

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
                if (logger.isDebugEnabled()) {
                    logger.debug("{} not found", relativePath);
                }
                return;
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
}