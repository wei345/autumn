package io.liuwei.autumn.data;

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
        static final Data EMPTY = new Data(
                TreeJson.EMPTY,
                Page.newEmptyPage("/"),
                Collections.emptyMap(),
                Collections.emptyMap());
        @NotNull
        private TreeJson treeJson;

        @NotNull
        private Map<String, Page> path2page;

        @NotNull
        private Map<String, Media> path2media;

        @NotNull
        private Page homepage;

        private long pageCountExcludeGenerated;

        Data(@NotNull TreeJson treeJson,
             @NotNull Page homepage,
             @NotNull Map<String, Page> path2page,
             @NotNull Map<String, Media> path2media) {
            this.treeJson = treeJson;
            this.homepage = homepage;
            this.path2page = path2page;
            this.path2media = path2media;
            this.pageCountExcludeGenerated = path2page.entrySet().stream()
                    .filter(entry -> !entry.getValue().isGenerated())
                    .count();
        }

        public TreeJson getTreeJson() {
            return treeJson;
        }

        public Map<String, Page> getPath2page() {
            return path2page;
        }

        public Map<String, Media> getPath2media() {
            return path2media;
        }

        public Page getHomepage() {
            return homepage;
        }

        public long getPageCountExcludeGenerated() {
            return pageCountExcludeGenerated;
        }

        @Override
        public String toString() {
            return String.format("page: %s, media: %s", path2page.size(), path2media.size());
        }
    }
}

