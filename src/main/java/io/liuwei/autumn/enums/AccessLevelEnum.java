package io.liuwei.autumn.enums;

/**
 * @author liuwei
 * @since 2021-07-07 16:34
 */
public enum AccessLevelEnum {
    PRIVATE, USER, PUBLIC;

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
