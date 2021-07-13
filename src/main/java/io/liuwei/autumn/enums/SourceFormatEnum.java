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
    OTHER(Collections.emptyList());

    private static final Map<String, SourceFormatEnum> EXT_2_VALUE_MAP;

    static {
        Map<String, SourceFormatEnum> map = new HashMap<>();
        for (SourceFormatEnum value : values()) {
            for (String ext : value.fileExtensions) {
                map.put(ext, value);
            }
        }
        EXT_2_VALUE_MAP = map;
    }

    @Getter
    private final List<String> fileExtensions;

    SourceFormatEnum(List<String> fileExtensions) {
        this.fileExtensions = fileExtensions;
    }

    public static SourceFormatEnum getByFileName(String fileName) {
        if (fileName == null) {
            return null;
        }
        String ext = FilenameUtils.getExtension(fileName).toLowerCase();
        return EXT_2_VALUE_MAP.getOrDefault(ext, OTHER);
    }
}
