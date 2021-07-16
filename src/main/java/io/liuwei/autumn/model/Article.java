package io.liuwei.autumn.model;

import io.liuwei.autumn.component.MediaRevisionResolver;
import io.liuwei.autumn.enums.AccessLevelEnum;
import lombok.Data;

import javax.annotation.Nullable;
import java.io.File;
import java.util.Date;
import java.util.Set;

/**
 * @author liuwei
 * @since 2021-07-07 15:45
 */
@Data
public class Article {

    /**
     * 在数据目录内的路径，以 / 开头，唯一
     */
    private String path;

    /**
     * path 最后一个斜线之后的部分
     */
    private String name;

    private Date created;

    private Date modified;

    private String category;

    private Set<String> tags;

    private AccessLevelEnum accessLevel;

    private String title;

    /**
     * 正文内容
     */
    private String content;

    /**
     * 源内容，如果 {@link #file} 不为 null 就是 {@link #file} 文件内容
     */
    private String source;

    private String sourceMd5;

    /**
     * 快照 ID
     * <p>
     *
     * @see MediaRevisionResolver#getSnapshotId(Article)
     */
    private String snapshotId;

    /**
     * 如果 {@link #file} 不为 null 就是 {@link #file} 的 lastModified
     */
    private Long lastModified;

    /**
     * 对应的文件
     */
    @Nullable
    private File file;

}
