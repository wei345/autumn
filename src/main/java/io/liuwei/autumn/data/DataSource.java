package io.liuwei.autumn.data;

import io.liuwei.autumn.domain.Media;
import io.liuwei.autumn.domain.Page;
import io.liuwei.autumn.model.RevisionContent;
import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;

import javax.validation.constraints.NotNull;
import java.util.Collections;
import java.util.Map;

/**
 * @author liuwei
 * Created by liuwei on 2018/11/19.
 */
@Component
@Getter
@Setter
public class DataSource {
    private volatile Data allData = Data.EMPTY;

    private volatile Data publishedData = Data.EMPTY;

    @Override
    public String toString() {
        return String.format("allData: %s, publishedData: %s", allData, publishedData);
    }

    @Getter
    public static class Data {
        static final Data EMPTY = new Data(
                RevisionContent.EMPTY_TREE_JSON,
                Page.newEmptyPage("/"),
                Collections.emptyMap(),
                Collections.emptyMap());
        @NotNull
        private final RevisionContent treeJson;

        @NotNull
        private final Map<String, Page> path2page;

        @NotNull
        private final Map<String, Media> path2media;

        @NotNull
        private final Page homepage;

        private final long pageCountExcludeGenerated;

        Data(@NotNull RevisionContent treeJson,
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

        @Override
        public String toString() {
            return String.format("page: %s, media: %s", path2page.size(), path2media.size());
        }
    }
}

