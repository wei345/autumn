package io.liuwei.autumn.data;

import java.io.File;

/**
 * @author liuwei
 * Created by liuwei on 2018/11/25.
 */
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

    public void setMd5(String md5) {
        this.md5 = md5;
    }

    public long getLastModified() {
        return lastModified;
    }

    public void setLastModified(long lastModified) {
        this.lastModified = lastModified;
    }

    public byte[] getContent() {
        return content;
    }

    public void setContent(byte[] content) {
        this.content = content;
    }

    public String getMd5() {
        return md5;
    }

    public File getFile() {
        return file;
    }

    public void setFile(File file) {
        this.file = file;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public String getEtag() {
        return etag;
    }

    public void setEtag(String etag) {
        this.etag = etag;
    }

    public String getVersionKeyValue() {
        return versionKeyValue;
    }

    public void setVersionKeyValue(String versionKeyValue) {
        this.versionKeyValue = versionKeyValue;
    }
}
