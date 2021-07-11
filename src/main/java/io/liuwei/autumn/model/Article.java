package io.liuwei.autumn.model;

import io.liuwei.autumn.enums.AccessLevelEnum;
import io.liuwei.autumn.search.model.PageHit;
import lombok.Data;

import javax.annotation.Nullable;
import java.io.File;
import java.util.Date;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author liuwei
 * @since 2021-07-07 15:45
 */
@Data
public class Article {
    /**
     * 路径，唯一
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

    private String content;

    /**
     * 源内容，如果 file 不为 null 就是 file 里的内容
     */
    private String source;

    private String sourceMd5;

    /**
     * 对应的文件
     */
    @Nullable
    private File file;

    // lazy init
    private volatile ConcurrentHashMap<String, PageHit> searchHitCache;

}
