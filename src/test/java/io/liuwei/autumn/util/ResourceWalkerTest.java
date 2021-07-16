package io.liuwei.autumn.util;

import io.liuwei.autumn.constant.Constants;
import org.apache.commons.lang3.StringUtils;
import org.junit.Test;

import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

import static java.nio.file.FileVisitResult.*;

/**
 * @author liuwei
 * Created by liuwei on 2018/12/6.
 */
public class ResourceWalkerTest {

    @Test
    public void walk() {
        ResourceWalker.walk("/static", new SimpleFileVisitor<Path>() {

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                if (file.getFileName().toString().startsWith(".")) {
                    return CONTINUE;
                }

                String path = file.toString();

                if (StringUtils.containsIgnoreCase(file.getFileSystem().getClass().getSimpleName(), "zip")) {

                    if (path.startsWith(Constants.BOOT_INF_CLASSES)) {
                        String relativePath = path.substring(Constants.BOOT_INF_CLASSES.length());
                        getClass().getResourceAsStream(relativePath);
                    }
                }
                return CONTINUE;
            }


        });
    }
}