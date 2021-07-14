package io.liuwei.autumn.component;

import io.liuwei.autumn.constant.Constants;
import io.liuwei.autumn.enums.RevisionErrorEnum;
import io.liuwei.autumn.manager.ArticleManager;
import io.liuwei.autumn.model.Article;
import io.liuwei.autumn.model.Media;
import io.liuwei.autumn.model.RevisionContent;
import io.liuwei.autumn.model.RevisionEtag;
import io.liuwei.autumn.util.IOUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.Cache;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.DigestUtils;
import org.springframework.util.unit.DataSize;

/**
 * revision 放在 url 参数里，格式:
 * 1. {majorVersion}.{md5 前 7 位}
 * 2. {majorVersion}.{时间戳}
 * <p>
 * ETag 格式：
 * 1. {majorVersion}.{md5}
 * 2. W/"{majorVersion}.{时间戳}"
 *
 * @author liuwei
 * @since 2021-07-08 13:02
 */
@SuppressWarnings("FieldMayBeFinal")
@Component
@Slf4j
public class MediaRevisionResolver {
    /**
     * 修改 etag version 会导致所有客户端页面缓存失效。
     * 某些情况下你可能想修改这个值，例如修改了 response CharacterEncoding
     */
    private int majorVersion = 1;

    @Autowired
    private ArticleManager articleManager;

    @Autowired
    @Qualifier("mediaCache")
    private Cache mediaCache;

    @Value("${autumn.cache.maxMediaSize}")
    private DataSize cacheMaxMediaSize;

    public static String getSnapshotId(Article article) {
        return article.getPath() + ":" + article.getSourceMd5().substring(0, 7);
    }

    /**
     * @return RevisionEtag 对象; 或 null 如果文件不存在或发生 IO 异常
     */
    public RevisionEtag getRevisionEtag(Media media) {
        // 大文件，使用 lastModified
        if (media.getFile().length() > cacheMaxMediaSize.toBytes()) {
            long lastModified = media.getFile().lastModified();
            if (lastModified == 0) {
                log.warn("media_file_not_found. media={}", media);
                return null;
            }
            return new RevisionEtag(getRevision(lastModified), getEtag(lastModified));
        }

        // 小文件，读取内容，计算 md5
        try {
            return new RevisionEtag(toRevisionContent(media));
        } catch (Cache.ValueRetrievalException e) {
            log.warn("get_revision_error. path=" + media.getPath(), e);
            return null;
        }
    }

    private String getRevision(String md5) {
        return majorVersion + "." + StringUtils.substring(md5, 0, 7);
    }

    private String getRevision(long timestamp) {
        return majorVersion + "." + timestamp;
    }

    private String getEtag(String md5) {
        return majorVersion + "." + md5;
    }

    private String getEtag(long timestamp) {
        return "W/\"" + majorVersion + "." + timestamp + "\"";
    }

    public String toRevisionUrl(String path) {
        Media media = articleManager.getMedia(path);
        if (media == null) {
            return toRevisionUrl(path, RevisionErrorEnum.MEDIA_NOT_FOUND.name());
        }
        RevisionEtag re = getRevisionEtag(media);
        if (re == null) {
            return toRevisionUrl(path, RevisionErrorEnum.IO_EXCEPTION.name());
        }
        return toRevisionUrl(path, re.getRevision());
    }

    public String toRevisionUrl(String path, String revision) {
        return path + "?" + Constants.REQUEST_PARAMETER_REVISION + "=" + revision;
    }

    /**
     * 读取文件内容，构造 RevisionContent 对象。
     *
     * @throws Cache.ValueRetrievalException 如果读取文件发生 IOException
     */
    private RevisionContent toRevisionContent(Media media) throws Cache.ValueRetrievalException {
        return mediaCache.get(
                media.getPath(),
                () -> toRevisionContent(IOUtil.toByteArray(media.getFile()), media.getMediaType()));
    }

    public RevisionContent toRevisionContent(byte[] bytes, MediaType mediaType) {
        String md5 = DigestUtils.md5DigestAsHex(bytes);
        RevisionContent rc = new RevisionContent(bytes, mediaType);
        rc.setMd5(md5);
        rc.setEtag(getEtag(md5));
        rc.setRevision(getRevision(md5));
        rc.setTimestamp(System.currentTimeMillis());
        return rc;
    }

}
