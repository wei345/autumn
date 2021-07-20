package io.liuwei.autumn.component;

import io.liuwei.autumn.constant.Constants;
import io.liuwei.autumn.model.Article;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * revision 是放在 url 参数里的，v={revision}，{revision} 格式:
 *
 * <ul>
 * <li>1. {majorVersion}.{md5 前 7 位} 或
 * <li>2. {majorVersion}.{时间戳}
 * </ul>
 * <p>
 * ETag 格式：
 * <ul>
 * <li>1. {majorVersion}.{md5} 或
 * <li>2. W/"{majorVersion}.{时间戳}"
 * </ul>
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

    /**
     * 如果设置了强 ETag，Tomcat 不压缩。
     * 所以如果启用压缩，我们设置弱 ETag。
     */
    @Value("${server.compression.enabled}")
    private boolean compressionEnabled;

    /**
     * 对压缩类型设置弱 ETag，对不压缩类型设置强 ETag。
     */
    @Value("${server.compression.mime-types}")
    private Set<String> compressionTypes;

    public static String getSnapshotId(Article article) {
        return article.getPath() + ":" + article.getSourceMd5().substring(0, 7);
    }

    public String getRevision(String md5) {
        return majorVersion + "." + StringUtils.substring(md5, 0, 7);
    }

    public String getRevision(long timestamp) {
        return majorVersion + "." + timestamp;
    }

    public String getEtag(String md5, MediaType mediaType) {
        String contentType = mediaType.getType() + "/" + mediaType.getSubtype();
        boolean isWeak = compressionEnabled && compressionTypes.contains(contentType);
        return etag(majorVersion + "." + md5, isWeak);
    }

    public String getEtag(long timestamp) {
        return etag(majorVersion + "." + timestamp, true);
    }

    private String etag(String value, boolean isWeak) {
        return isWeak ? "W/\"" + value + "\"" : value;
    }

    public String toRevisionUrl(String path, String revision) {
        return path + "?" + Constants.REQUEST_PARAMETER_REVISION + "=" + revision;
    }

}
