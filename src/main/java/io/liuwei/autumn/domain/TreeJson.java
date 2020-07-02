package io.liuwei.autumn.domain;

import lombok.Getter;
import org.springframework.util.DigestUtils;
import io.liuwei.autumn.util.WebUtil;

import java.nio.charset.StandardCharsets;

@Getter
public class TreeJson {
    public static final TreeJson EMPTY = new TreeJson("{}");
    private final String json;
    private final String md5;
    private final String versionKeyValue;
    private final String etag;

    public TreeJson(String json) {
        this.json = json;
        this.md5 = DigestUtils.md5DigestAsHex(json.getBytes(StandardCharsets.UTF_8));
        this.etag = WebUtil.getEtag(md5);
        this.versionKeyValue = WebUtil.getVersionKeyValue(md5);
    }
}