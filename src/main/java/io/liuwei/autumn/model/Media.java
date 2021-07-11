package io.liuwei.autumn.model;

import io.liuwei.autumn.enums.AccessLevelEnum;
import lombok.Data;

import java.io.File;

/**
 * @author liuwei
 * @since 2021-07-08 23:01
 */
@Data
public class Media {

    private String path;

    private File file;

    private AccessLevelEnum accessLevel;
}
