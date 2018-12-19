package xyz.liuw.autumn.data;

import java.io.File;

/**
 * @author liuwei
 * Created by liuwei on 2018/11/25.
 */
public class Media {
    private File file;
    private volatile byte[] content; // file content
    private volatile String md5; // content md5
    private volatile String mimeType;
    private long lastModified; // file last modified

    public Media() {

    }

    public Media(File file) {
        this.file = file;
        this.lastModified = file.lastModified();
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

    public void setMd5(String md5) {
        this.md5 = md5;
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
}
