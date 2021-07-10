package io.liuwei.autumn.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;

/**
 * @author liuwei
 * @since 2021-07-10 17:21
 */
@Data
public class DataInfo {

    private Integer fileCount;

    private Integer articleCount;

    private Integer userAccessibleArticleCount;

    private Integer anonAccessibleArticleCount;

    private Long timeCostInMills;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date time;

}
