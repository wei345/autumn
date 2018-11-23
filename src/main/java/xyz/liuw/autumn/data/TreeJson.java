package xyz.liuw.autumn.data;

import org.springframework.util.DigestUtils;

import java.nio.charset.StandardCharsets;

public class TreeJson {
    static final TreeJson EMPTY = new TreeJson("{}");
    private String json;
    private String md5;

    TreeJson(String json) {
        this.json = json;
        this.md5 = DigestUtils.md5DigestAsHex(json.getBytes(StandardCharsets.UTF_8));
    }

    public String getJson() {
        return json;
    }

    public void setJson(String json) {
        this.json = json;
    }

    public String getMd5() {
        return md5;
    }

    public void setMd5(String md5) {
        this.md5 = md5;
    }
}