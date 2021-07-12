package io.liuwei.autumn.component;

import io.liuwei.autumn.constant.Constants;
import io.liuwei.autumn.manager.ArticleManager;
import io.liuwei.autumn.model.Article;
import io.liuwei.autumn.model.Media;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author liuwei
 * @since 2021-07-08 13:02
 */
@SuppressWarnings("FieldMayBeFinal")
@Component
public class MediaRevisionResolver {
    // 修改 etag version 会导致所有客户端页面缓存失效。某些情况下你可能想修改这个值，例如修改了 response CharacterEncoding
    private int majorVersion = 1;

    @Autowired
    private ArticleManager articleManager;

    public static String getSnapshotId(Article article) {
        return article.getPath() + ":" + article.getSourceMd5().substring(0, 7);
    }

    /**
     * @param messageDigest 消息摘要结果，如 md5
     */
    public String getRevisionByDigest(String messageDigest) {
        return majorVersion + "." + StringUtils.substring(messageDigest, 0, 7);
    }

    public String getRevisionByTimestamp(long messageTimestamp) {
        return majorVersion + "." + messageTimestamp;
    }

    public String getRevisionByMediaPath(String path) {
        Media media = articleManager.getMedia(path);
        if (media == null) {
            return "";
        }
        return getRevisionByTimestamp(media.getFile().lastModified());
    }

    public String getEtag(String messageDigest) {
        return majorVersion + "." + messageDigest;
    }

    public String getMediaRevisionUrl(String path) {
        return toRevisionUrl(path, getRevisionByMediaPath(path));
    }

    public String toRevisionUrl(String path, String revision) {
        return path + "?" + Constants.REQUEST_PARAMETER_REVISION + "=" + revision;
    }

}
