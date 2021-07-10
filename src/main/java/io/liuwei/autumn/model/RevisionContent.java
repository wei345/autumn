package io.liuwei.autumn.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RevisionContent {
    private String content;
    private String md5;
    private String revision;
    private String etag;
    private Long timestamp;

    public RevisionContent(String content) {
        this.content = content;
    }
}