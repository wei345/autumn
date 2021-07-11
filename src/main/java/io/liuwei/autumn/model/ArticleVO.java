package io.liuwei.autumn.model;

import io.liuwei.autumn.enums.AccessLevelEnum;
import lombok.Data;

import java.util.Date;
import java.util.Set;

/**
 * @author liuwei
 * @since 2021-07-08 08:48
 */
@Data
public class ArticleVO {
    private String path;

    private String name;

    private Date created;

    private Date modified;

    private String category;

    private Set<String> tags;

    private AccessLevelEnum accessLevel;

    private String title;

    private String content;

    private String source;

    private String sourceMd5;

    private String titleHtml;

    private String contentHtml;

    private String tocHtml;
}
