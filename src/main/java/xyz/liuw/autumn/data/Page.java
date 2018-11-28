package xyz.liuw.autumn.data;

import xyz.liuw.autumn.search.PageHit;

import java.util.Date;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author liuwei
 * Created by liuwei on 2018/11/12.
 */
public class Page {

    private Date created;
    private Date modified;
    private String category;
    private Set<String> tags;
    private boolean published; // default false
    private String title;
    private String body;
    private volatile String bodyHtml;
    private String source; // file content
    private long lastModified; // file last modified
    private volatile ViewCache userViewCache; // 已登录用户页面缓存
    private volatile ViewCache guestViewCache; // 未登录用户页面缓存
    private String path;
    private volatile ConcurrentHashMap<String, PageHit> searchHitCache;

    public static class ViewCache {
        private byte[] content;
        private String etag;
        private long templateLastModified;

        public ViewCache(byte[] content, String etag, long templateLastModified) {
            this.content = content;
            this.etag = etag;
            this.templateLastModified = templateLastModified;
        }

        public byte[] getContent() {
            return content;
        }

        public String getEtag() {
            return etag;
        }

        public long getTemplateLastModified() {
            return templateLastModified;
        }
    }

    public Date getCreated() {
        return created;
    }

    public void setCreated(Date created) {
        this.created = created;
    }

    public Date getModified() {
        return modified;
    }

    public void setModified(Date modified) {
        this.modified = modified;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public Set<String> getTags() {
        return tags;
    }

    public void setTags(Set<String> tags) {
        this.tags = tags;
    }

    public boolean isPublished() {
        return published;
    }

    public void setPublished(boolean published) {
        this.published = published;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getBodyHtml() {
        return bodyHtml;
    }

    public void setBodyHtml(String bodyHtml) {
        this.bodyHtml = bodyHtml;
    }

    public long getLastModified() {
        return lastModified;
    }

    public void setLastModified(long lastModified) {
        this.lastModified = lastModified;
    }

    public ViewCache getUserViewCache() {
        return userViewCache;
    }

    public void setUserViewCache(ViewCache userViewCache) {
        this.userViewCache = userViewCache;
    }

    public ViewCache getGuestViewCache() {
        return guestViewCache;
    }

    public void setGuestViewCache(ViewCache guestViewCache) {
        this.guestViewCache = guestViewCache;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public ConcurrentHashMap<String, PageHit> getSearchHitCache() {
        return searchHitCache;
    }

    public void setSearchHitCache(ConcurrentHashMap<String, PageHit> searchHitCache) {
        this.searchHitCache = searchHitCache;
    }
}
