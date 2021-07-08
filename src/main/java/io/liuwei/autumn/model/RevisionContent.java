package io.liuwei.autumn.model;

import lombok.Getter;
import lombok.Setter;
import org.springframework.util.DigestUtils;
import io.liuwei.autumn.util.WebUtil;

import java.nio.charset.StandardCharsets;

@Getter
@Setter
public class RevisionContent {
    public static final RevisionContent EMPTY_TREE_JSON = new RevisionContent("{}");
    private String content;
    private String md5;
    private String revision;
    private String versionKeyValue;
    private String etag;
    private Long timestamp;

    public RevisionContent(String content) {
        this.content = content;
        // todo delete the following code
        this.md5 = DigestUtils.md5DigestAsHex(content.getBytes(StandardCharsets.UTF_8));
        this.etag = WebUtil.getEtag(md5);
        this.versionKeyValue = WebUtil.getVersionKeyValue(md5);
    }
}