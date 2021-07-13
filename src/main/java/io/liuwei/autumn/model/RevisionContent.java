package io.liuwei.autumn.model;

import lombok.Getter;
import lombok.Setter;
import org.springframework.http.MediaType;

@Getter
@Setter
public class RevisionContent {
    private byte[] content;
    private MediaType mediaType;
    private String md5;
    private String revision;
    private String etag;
    /**
     * 读取文件内容的时间
     */
    private Long timestamp;

    public RevisionContent(byte[] content, MediaType mediaType) {
        this.content = content;
        this.mediaType = mediaType;
    }
}