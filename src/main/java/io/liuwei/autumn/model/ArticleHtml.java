package io.liuwei.autumn.model;

import lombok.Getter;
import lombok.Setter;

/**
 * @author liuwei
 * @since 2021-07-08 01:06
 */
@Getter
@Setter
public class ArticleHtml {
    private String toc;
    private String title;
    private String content;
    private long time;

    public ArticleHtml(String title, String toc, String content) {
        this.toc = toc;
        this.title = title;
        this.content = content;
        this.time = System.currentTimeMillis();
    }
}
