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
    private String title;
    private String titleHtml;
    private String tocHtml;
    private String contentHtml;
    private long time;

    public ArticleHtml(String title, String titleHtml, String tocHtml, String contentHtml) {
        this.title = title;
        this.tocHtml = tocHtml;
        this.titleHtml = titleHtml;
        this.contentHtml = contentHtml;
        this.time = System.currentTimeMillis();
    }
}
