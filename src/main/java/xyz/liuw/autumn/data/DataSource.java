package xyz.liuw.autumn.data;

import org.springframework.stereotype.Component;

import javax.validation.constraints.NotNull;
import java.util.Collections;
import java.util.Date;
import java.util.Map;

/**
 * @author liuwei
 * Created by liuwei on 2018/11/19.
 */
@Component
public class DataSource {
    private volatile Data allData = Data.EMPTY;
    private volatile Data publishedData = Data.EMPTY;

    public Data getAllData() {
        return allData;
    }

    void setAllData(Data allData) {
        this.allData = allData;
    }

    public Data getPublishedData() {
        return publishedData;
    }

    void setPublishedData(Data publishedData) {
        this.publishedData = publishedData;
    }

    @Override
    public String toString() {
        return String.format("allData: %s, publishedData: %s",
                allData, publishedData);
    }

    public static class Data {
        static final Data EMPTY;

        static {
            String title = "Home";
            String body = "Welcome";
            Date now = new Date(0);
            Page welcome = new Page();
            welcome.setCreated(now);
            welcome.setModified(now);
            welcome.setPublished(true);
            welcome.setBody(body);
            welcome.setSource(body);
            welcome.setTitle(title);
            welcome.setLastModified(now.getTime());
            welcome.setPath("/");

            EMPTY = new Data(TreeJson.EMPTY, welcome, Collections.emptyMap(), Collections.emptyMap());
        }

        @NotNull
        private TreeJson treeJson;
        // path -> Page
        @NotNull
        private Map<String, Page> pageMap;
        // path -> Media
        @NotNull
        private Map<String, Media> mediaMap;

        @NotNull
        private Page homepage;
        @NotNull
        private volatile Page helpPage;

        Data(@NotNull TreeJson treeJson,
             @NotNull Page homepage,
             @NotNull Map<String, Page> pageMap,
             @NotNull Map<String, Media> mediaMap) {
            this.treeJson = treeJson;
            this.homepage = homepage;
            this.pageMap = pageMap;
            this.mediaMap = mediaMap;
        }

        public TreeJson getTreeJson() {
            return treeJson;
        }

        public Map<String, Page> getPageMap() {
            return pageMap;
        }

        public Map<String, Media> getMediaMap() {
            return mediaMap;
        }

        public Page getHomepage() {
            return homepage;
        }

        public Page getHelpPage() {
            return helpPage;
        }

        public void setHelpPage(Page helpPage) {
            this.helpPage = helpPage;
        }

        @Override
        public String toString() {
            return String.format("page: %s, media: %s", pageMap.size(), mediaMap.size());
        }
    }
}

