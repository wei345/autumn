package io.liuwei.autumn.model;

import io.liuwei.autumn.enums.AccessLevelEnum;
import lombok.Data;
import org.springframework.http.MediaType;

import java.io.File;

/**
 * @author liuwei
 * @since 2021-07-08 23:01
 */
@Data
public class Media {

    /**
     * 在数据目录内的路径，以 / 开头，唯一
     */
    private String path;

    private MediaType mediaType;

    private AccessLevelEnum accessLevel;

    /**
     * {@link #file} 的 lastModified
     */
    private Long lastModified;

    private File file;
}
