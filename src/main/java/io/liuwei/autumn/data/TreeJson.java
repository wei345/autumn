package io.liuwei.autumn.data;

import org.springframework.util.DigestUtils;
import io.liuwei.autumn.util.WebUtil;

import java.nio.charset.StandardCharsets;

public class TreeJson {
    static final TreeJson EMPTY = new TreeJson("{}");
    private String json;
    private String md5;
    private String versionKeyValue;
    private volatile String etag;

    TreeJson(String json) {
        this.json = json;
        this.md5 = DigestUtils.md5DigestAsHex(json.getBytes(StandardCharsets.UTF_8));
        this.etag = WebUtil.getEtag(md5);
        this.versionKeyValue = WebUtil.getVersionKeyValue(md5);
    }

    public String getJson() {
        return json;
    }

    public String getMd5() {
        return md5;
    }

    public String getVersionKeyValue() {
        return versionKeyValue;
    }

    public String getEtag() {
        return etag;
    }

}