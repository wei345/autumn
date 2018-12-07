package xyz.liuw.autumn.manage;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.boot.actuate.endpoint.annotation.WriteOperation;
import org.springframework.stereotype.Component;
import xyz.liuw.autumn.data.DataLoader;
import xyz.liuw.autumn.data.DataSource;

/**
 * @author liuwei
 * Created by liuwei on 2018/12/7.
 */
@Endpoint(id = "data")
@Component
public class DataEndpoint {

    @Autowired
    private DataLoader dataLoader;

    @Autowired
    private DataSource dataSource;

    @ReadOperation
    public DataStat getDataStat() {
        DataSource.Data allData = dataSource.getAllData();
        DataSource.Data publishedData = dataSource.getPublishedData();

        Stat allStat = new Stat();
        allStat.setPageCount(allData.getPageMap().size());
        allStat.setMediaCount(allData.getMediaMap().size());
        allStat.setTreeJsonLength(allData.getTreeJson().getJson().length());

        Stat publishedStat = new Stat();
        publishedStat.setPageCount(publishedData.getPageMap().size());
        publishedStat.setMediaCount(publishedData.getMediaMap().size());
        publishedStat.setTreeJsonLength(publishedData.getTreeJson().getJson().length());

        return new DataStat(allStat, publishedStat);
    }

    @WriteOperation
    public String reload() {
        long start = System.currentTimeMillis();
        dataLoader.load();
        long cost = System.currentTimeMillis() - start;
        return String.format("Reloaded in %s ms. %s", cost, dataSource);
    }

    public static class DataStat {
        private Stat all;
        private Stat published;

        DataStat(Stat all, Stat published) {
            this.all = all;
            this.published = published;
        }

        public Stat getAll() {
            return all;
        }

        public Stat getPublished() {
            return published;
        }
    }

    public static class Stat {
        private int pageCount;
        private int mediaCount;
        private int treeJsonLength;

        public int getPageCount() {
            return pageCount;
        }

        void setPageCount(int pageCount) {
            this.pageCount = pageCount;
        }

        public int getMediaCount() {
            return mediaCount;
        }

        void setMediaCount(int mediaCount) {
            this.mediaCount = mediaCount;
        }

        public int getTreeJsonLength() {
            return treeJsonLength;
        }

        void setTreeJsonLength(int treeJsonLength) {
            this.treeJsonLength = treeJsonLength;
        }
    }
}
