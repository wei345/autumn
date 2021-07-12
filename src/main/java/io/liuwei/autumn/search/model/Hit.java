package io.liuwei.autumn.search.model;

import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
public class Hit {
    private int start;
    private int end;
    private String str;

    public Hit(int start, int end, String str) {
        this.start = start;
        this.end = end;
        this.str = str;
    }

}