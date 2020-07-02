package io.liuwei.autumn.domain;

import lombok.Getter;
import lombok.Setter;

/**
 * @author liuwei
 * Created by liuwei on 2019/1/15.
 */
@Getter
@Setter
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
}
