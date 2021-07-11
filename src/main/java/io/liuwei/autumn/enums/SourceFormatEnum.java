package io.liuwei.autumn.enums;

import lombok.Getter;
import org.apache.commons.io.FilenameUtils;

import java.util.*;

/**
 * @author liuwei
 * @since 2021-07-07 16:45
 */
public enum SourceFormatEnum {
    ASCIIDOC(Arrays.asList("adoc", "asciidoc", "asc")),
    MARKDOWN(Arrays.asList("md", "markdown")),
    OTHER(Collections.emptyList());

    @Getter
    private final List<String> fileExtensions;

    private static final Map<String, SourceFormatEnum> FILE_EXTENSION_MAP;

    static {
        FILE_EXTENSION_MAP = new HashMap<>();
        for (SourceFormatEnum sf : values()) {
            for (String ext : sf.fileExtensions) {
                FILE_EXTENSION_MAP.put(ext, sf);
            }
        }
    }

    SourceFormatEnum(List<String> fileExtensions) {
        this.fileExtensions = fileExtensions;
    }

    public static SourceFormatEnum getByFileName(String fileName) {
        if (fileName == null) {
            return null;
        }
        String ext = FilenameUtils.getExtension(fileName).toLowerCase();
        return FILE_EXTENSION_MAP.getOrDefault(ext, OTHER);
    }
}
