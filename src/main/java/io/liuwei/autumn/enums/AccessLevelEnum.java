package io.liuwei.autumn.enums;

import lombok.Getter;

/**
 * @author liuwei
 * @since 2021-07-07 16:34
 */
public enum AccessLevelEnum {
    PRIVATE(0), USER(1), PUBLIC(2);

    @Getter
    private final int level;

    AccessLevelEnum(int level) {
        this.level = level;
    }

    public static AccessLevelEnum of(String value, AccessLevelEnum defaultVal) {
        if (value == null) {
            return defaultVal;
        }
        value = value.toUpperCase();
        for (AccessLevelEnum al : values()) {
            if (al.name().equals(value)) {
                return al;
            }
        }
        return defaultVal;
    }
}
