package io.liuwei.autumn.manager;

import io.liuwei.autumn.component.MediaRevisionResolver;
import io.liuwei.autumn.config.AppProperties;
import io.liuwei.autumn.enums.RevisionErrorEnum;
import io.liuwei.autumn.model.Media;
import io.liuwei.autumn.model.RevisionContent;
import io.liuwei.autumn.model.RevisionEtag;
import io.liuwei.autumn.util.IOUtil;
import io.liuwei.autumn.util.Md5Util;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.Cache;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

/**
 * @author liuwei
 * @since 2021-07-20 22:47
 */
@Component
@Slf4j
public class RevisionContentManager {

    private final MediaManager mediaManager;

    private final MediaRevisionResolver mediaRevisionResolver;

    private final Cache mediaCache;

    private final AppProperties.Cache cache;

    public RevisionContentManager(MediaManager mediaManager,
                                  MediaRevisionResolver mediaRevisionResolver,
                                  @Qualifier("mediaCache") Cache mediaCache,
                                  AppProperties.Cache cache) {
        this.mediaManager = mediaManager;
        this.mediaRevisionResolver = mediaRevisionResolver;
        this.mediaCache = mediaCache;
        this.cache = cache;
    }

    public String toRevisionUrl(String path) {
        Media media = mediaManager.getMedia(path);
        if (media == null) {
            return mediaRevisionResolver.toRevisionUrl(path, RevisionErrorEnum.MEDIA_NOT_FOUND.name());
        }
        RevisionEtag re = getRevisionEtag(media);
        if (re == null) {
            return mediaRevisionResolver.toRevisionUrl(path, RevisionErrorEnum.IO_EXCEPTION.name());
        }
        return mediaRevisionResolver.toRevisionUrl(path, re.getRevision());
    }

    /**
     * @return RevisionEtag 对象; 或 null 如果文件不存在或发生 IO 异常
     */
    public RevisionEtag getRevisionEtag(Media media) {
        // 大文件，使用 lastModified
        if (media.getFile().length() > cache.getMaxMediaSize().toBytes()) {
            long lastModified = media.getFile().lastModified();
            if (lastModified == 0) {
                log.warn("media_file_not_found. media={}", media);
                return null;
            }
            return new RevisionEtag(
                    mediaRevisionResolver.getRevision(lastModified),
                    mediaRevisionResolver.getEtag(lastModified));
        }

        // 小文件，读取内容，计算 md5
        try {
            return new RevisionEtag(toRevisionContent(media));
        } catch (Cache.ValueRetrievalException e) {
            log.warn("get_revision_error. path=" + media.getPath(), e);
            return null;
        }
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

    public RevisionContent toRevisionContent(byte[] content, MediaType mediaType) {
        // 计算 md5 包含内容类型，如果内容类型变化，md5 也会变化
        String md5 = Md5Util.md5DigestAsHex(mediaType, content);
        RevisionContent rc = new RevisionContent(content, mediaType);
        rc.setMd5(md5);
        rc.setEtag(mediaRevisionResolver.getEtag(md5, mediaType));
        rc.setRevision(mediaRevisionResolver.getRevision(md5));
        rc.setTimestamp(System.currentTimeMillis());
        return rc;
    }
}
