package xyz.liuw.autumn.data;

import org.springframework.util.DigestUtils;

import java.nio.charset.StandardCharsets;

public class TreeJson {
    static final TreeJson EMPTY = new TreeJson("{}");
    private String json;
    private String md5;
    private String version;
    private volatile String etag;

    TreeJson(String json) {
        this.json = json;
        this.md5 = DigestUtils.md5DigestAsHex(json.getBytes(StandardCharsets.UTF_8));
        this.version = md5.substring(0, 7);
    }

    public String getJson() {
        return json;
    }

    public String getMd5() {
        return md5;
    }

    public String getVersion() {
        return version;
    }

    public String getEtag() {
        return etag;
    }

    public void setEtag(String etag) {
        this.etag = etag;
    }
}