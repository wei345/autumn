package io.liuwei.autumn.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.liuwei.autumn.enums.AccessLevelEnum;
import lombok.Data;

import java.util.Date;

/**
 * @author liuwei
 * @since 2021-07-10 17:21
 */
@Data
public class DataInfo {
    /**
     * 数据目录
     */
    private String dataDir;

    /**
     * 总文件数
     */
    private Integer file;

    /**
     * 文章数
     */
    private Integer article;

    /**
     * 仅 {@link AccessLevelEnum#OWNER} 级别用户可见的文章数
     */
    private Integer ownerOnlyAccessible;

    /**
     * {@link AccessLevelEnum#USER} 级别用户可见的文章数
     */
    private Integer userAccessible;

    /**
     * {@link AccessLevelEnum#ANON} 级别用户可见的文章数
     */
    private Integer anonAccessible;

    /**
     * 加载数据花费的时间，毫秒
     */
    private Long cost;

    /**
     * 加载数据的时间
     */
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date time;

}
