package io.liuwei.autumn.domain;

import io.liuwei.autumn.enums.SourceFormatEnum;
import io.liuwei.autumn.model.ContentHtml;
import io.liuwei.autumn.search.PageHit;
import lombok.Getter;
import lombok.Setter;

import java.io.File;
import java.util.Collections;
import java.util.Date;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author liuwei
 * Created by liuwei on 2018/11/12.
 */

@Getter
@Setter
public class Page {
    private Date created;
    private Date modified;
    private String category;
    private Set<String> tags;
    private boolean published; // default false
    private boolean archived; // 是否已归档
    private String name; // path 最后一个斜线之后的部分
    private String title;
    private String body;
    private volatile ContentHtml pageHtml;
    private String source; // file content
    private SourceFormatEnum sourceFormat;
    private long fileLastModified;
    private File file;
    private volatile ViewCache userViewCache; // 已登录用户页面缓存
    private volatile ViewCache guestViewCache; // 未登录用户页面缓存
    private String path; // 页面路径，以 '/' 开头，无后缀名。如：/java/idea
    // searchStr -> PageHit
    private volatile ConcurrentHashMap<String, PageHit> searchHitCache;
    private volatile String sourceMd5;

    private boolean blog; // if file name like yyyy-MM-dd-xxx
    private String blogName; // blog file name without date and extension
    private String blogPath; // e.g. /2018/11/12/xxx
    private Date blogDate; // 从路径里解析出来的日期，如 /path/to/2018-11-12-xxx 会得到 2018-11-12

    private boolean generated;

    public static Page newEmptyPage(String path) {
        String title = "";
        String body = "";
        Date now = new Date(0);

        Page page = new Page();
        page.setCreated(now);
        page.setModified(now);
        page.setPublished(true);
        page.setBody(body);
        page.setSource(body);
        page.setTitle(title);
        page.setFileLastModified(now.getTime());
        page.setTags(Collections.emptySet());
        page.setPath(path);
        page.setSourceFormat(SourceFormatEnum.ASCIIDOC);
        return page;
    }

    public static class ViewCache {
        private final byte[] content;
        private final String etag;
        private final long time;

        public ViewCache(byte[] content, String etag) {
            this.content = content;
            this.etag = etag;
            this.time = System.currentTimeMillis();
        }

        public byte[] getContent() {
            return content;
        }

        public String getEtag() {
            return etag;
        }

        public long getTime() {
            return time;
        }
    }

}
