package io.liuwei.autumn;

import io.liuwei.autumn.model.Media;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author liuwei
 * @since 2021-07-08 13:02
 */
@Component
public class MediaRevisionResolver {
    // 修改 etag version 会导致所有客户端页面缓存失效。某些情况下你可能想修改这个值，例如修改了 response CharacterEncoding
    private int majorVersion = 1;

    @Autowired
    private ArticleManager articleManager;

    /**
     * @param messageDigest 消息摘要结果，如 md5
     */
    public String getRevision(String messageDigest) {
        return majorVersion + "." + StringUtils.substring(messageDigest, 0, 7);
    }

    public String getRevision(long messageTimestamp) {
        return majorVersion + "." + messageTimestamp;
    }

    public String getMediaRevision(String path) {
        Media media = articleManager.getMedia(path);
        if (media == null) {
            return null;
        }
        return getRevision(media.getFile().lastModified());
    }

    public String getEtag(String messageDigest) {
        return majorVersion + "." + messageDigest;
    }

    public String getRevisionParamName() {
        return "v";
    }
}
