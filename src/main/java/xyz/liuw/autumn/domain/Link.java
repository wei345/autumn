package xyz.liuw.autumn.domain;

/**
 * @author liuwei
 * Created by liuwei on 2019/1/15.
 */
public class Link {
    private String text;
    private String href;

    public Link(String text) {
        this.text = text;
    }

    public Link(String text, String href) {
        this.text = text;
        this.href = href;
    }

    public String getText() {
        return text;
    }

    public String getHref() {
        return href;
    }

    public void setText(String text) {
        this.text = text;
    }

    public void setHref(String href) {
        this.href = href;
    }
}
