package io.liuwei.autumn.model;

/**
 * @author liuwei
 * @since 2021-07-08 01:06
 */
public class ArticleHtml {
    private final String toc;
    private final String title;
    private final String content;
    private final long time;

    public ArticleHtml(String toc, String title, String content) {
        this.toc = toc;
        this.title = title;
        this.content = content;
        this.time = System.currentTimeMillis();
    }

    public String getToc() {
        return toc;
    }

    public String getContent() {
        return content;
    }

    public long getTime() {
        return time;
    }

    public String getTitle() {
        return title;
    }
}
