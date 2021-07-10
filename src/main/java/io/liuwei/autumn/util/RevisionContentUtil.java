package io.liuwei.autumn.util;

import io.liuwei.autumn.component.MediaRevisionResolver;
import io.liuwei.autumn.model.RevisionContent;
import org.springframework.util.DigestUtils;

import java.nio.charset.StandardCharsets;

/**
 * @author liuwei
 * @since 2021-07-08 19:22
 */
public class RevisionContentUtil {

    public static RevisionContent newRevisionContent(String content, MediaRevisionResolver mediaRevisionResolver) {
        String md5 = DigestUtils.md5DigestAsHex(content.getBytes(StandardCharsets.UTF_8));
        RevisionContent rc = new RevisionContent(content);
        rc.setMd5(md5);
        rc.setEtag(mediaRevisionResolver.getEtag(md5));
        rc.setRevision(mediaRevisionResolver.getRevisionByDigest(md5));
        rc.setTimestamp(System.currentTimeMillis());
        return rc;
    }
}
