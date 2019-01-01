package xyz.liuw.autumn.data;

import xyz.liuw.autumn.search.PageHit;

import java.util.Collections;
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
    private boolean archived; // 是否已归档
    private String name; // file name without extension
    private String title;
    private String body;
    private volatile HtmlCache htmlCache;
    private String source; // file content
    private long lastModified; // file last modified
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

    static Page newEmptyPage(String path) {
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
        page.setLastModified(now.getTime());
        page.setTags(Collections.emptySet());
        page.setPath(path);
        return page;
    }

    public static class HtmlCache {
        private String content;
        private long time;

        public HtmlCache(String content) {
            this.content = content;
            this.time = System.currentTimeMillis();
        }

        public String getContent() {
            return content;
        }

        public long getTime() {
            return time;
        }
    }

    public static class ViewCache {
        private byte[] content;
        private String etag;
        private long time;

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

    public HtmlCache getHtmlCache() {
        return htmlCache;
    }

    public void setHtmlCache(HtmlCache htmlCache) {
        this.htmlCache = htmlCache;
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

    public String getSourceMd5() {
        return sourceMd5;
    }

    public void setSourceMd5(String sourceMd5) {
        this.sourceMd5 = sourceMd5;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isArchived() {
        return archived;
    }

    public void setArchived(boolean archived) {
        this.archived = archived;
    }

    public boolean isBlog() {
        return blog;
    }

    public void setBlog(boolean blog) {
        this.blog = blog;
    }

    public String getBlogPath() {
        return blogPath;
    }

    public void setBlogPath(String blogPath) {
        this.blogPath = blogPath;
    }

    public Date getBlogDate() {
        return blogDate;
    }

    public void setBlogDate(Date blogDate) {
        this.blogDate = blogDate;
    }

    public String getBlogName() {
        return blogName;
    }

    public void setBlogName(String blogName) {
        this.blogName = blogName;
    }
}
