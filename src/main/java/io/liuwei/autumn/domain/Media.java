package io.liuwei.autumn.domain;

import lombok.Getter;
import lombok.Setter;

import java.io.File;

/**
 * @author liuwei
 * Created by liuwei on 2018/11/25.
 */
@Getter
@Setter
public class Media {
    private File file;
    private byte[] content; // file content
    private String md5; // content md5
    private String etag;
    private String versionKeyValue;
    private String mimeType;
    private long lastModified; // file last modified

    public Media() {

    }

    public Media(File file) {
        this.file = file;
        this.lastModified = file.lastModified();
    }
}
