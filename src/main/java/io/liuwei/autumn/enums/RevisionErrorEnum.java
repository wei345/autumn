package io.liuwei.autumn.enums;

/**
 * @author liuwei
 * @since 2021-07-13 23:20
 */
public enum RevisionErrorEnum {
    MEDIA_NOT_FOUND, IO_EXCEPTION;

    public static RevisionErrorEnum of(String value) {
        if (value == null) {
            return null;
        }
        value = value.toUpperCase();
        for (RevisionErrorEnum v : values()) {
            if (v.name().equals(value)) {
                return v;
            }
        }
        return null;
    }
}
