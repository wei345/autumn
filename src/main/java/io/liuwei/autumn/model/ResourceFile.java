package io.liuwei.autumn.model;

import lombok.Getter;
import lombok.Setter;
import org.springframework.http.MediaType;

import static java.nio.charset.StandardCharsets.*;

/**
 * @author liuwei
 * @since 2021-07-11 16:19
 */
@Getter
@Setter
public class ResourceFile {
    private byte[] content;
    private String md5; // content md5
    private MediaType mediaType;
    private long lastModified; // file last modified
    private String path; // classpath

    public String getContentAsString() {
        return new String(content, UTF_8);
    }

}
