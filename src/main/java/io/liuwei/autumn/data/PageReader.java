package io.liuwei.autumn.data;

import javax.validation.constraints.NotNull;
import java.io.File;

/**
 * @author liuwei
 * @since 2020-07-01 17:31
 */
public interface PageReader {
    Page toPage(File file, String path);

    Page toPage(@NotNull String text, @NotNull String path, long fileLastModified);
}
