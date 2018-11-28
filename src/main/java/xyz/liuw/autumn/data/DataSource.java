package xyz.liuw.autumn.data;

import org.springframework.stereotype.Component;

import javax.validation.constraints.NotNull;
import java.util.Collections;
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
        static final Data EMPTY = new Data(TreeJson.EMPTY, Collections.emptyMap(), Collections.emptyMap());

        @NotNull
        private TreeJson treeJson;
        // path -> Page
        @NotNull
        private Map<String, Page> pageMap;
        // path -> Media
        @NotNull
        private Map<String, Media> mediaMap;

        Data(@NotNull TreeJson treeJson,
             @NotNull Map<String, Page> pageMap,
             @NotNull Map<String, Media> mediaMap) {
            this.treeJson = treeJson;
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

        @Override
        public String toString() {
            return String.format("treeJson length %s, pageMap size %s, mediaMap size %s",
                    treeJson.getJson().length(), pageMap.size(), mediaMap.size());
        }
    }
}

