package io.liuwei.autumn.enums;

import lombok.Getter;

/**
 * @author liuwei
 * @since 2021-07-07 16:34
 */
public enum AccessLevelEnum {
    OWNER(0), USER(1), ANON(2);

    @Getter
    private final int level;

    AccessLevelEnum(int level) {
        this.level = level;
    }

    public static AccessLevelEnum of(String name, AccessLevelEnum defaultVal) {
        if (name == null) {
            return defaultVal;
        }
        name = name.toUpperCase();
        for (AccessLevelEnum value : values()) {
            if (value.name().equals(name)) {
                return value;
            }
        }
        return defaultVal;
    }

    /**
     * 给定级别的用户是否可以访问此级别的资源
     */
    public boolean allow(AccessLevelEnum accessLevel) {
        return accessLevel != null && accessLevel.getLevel() <= level;
    }
}
