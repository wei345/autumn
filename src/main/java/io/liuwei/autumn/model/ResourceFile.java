package io.liuwei.autumn.model;

import lombok.Getter;
import lombok.Setter;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * @author liuwei
 * @since 2021-07-11 16:19
 */
@Getter
@Setter
public class ResourceFile {
    private byte[] content;
    private String md5; // content md5
    private String mimeType;
    private long lastModified; // file last modified
    private String path; // classpath

    public String getContentString() {
        return new String(content, UTF_8);
    }

}
