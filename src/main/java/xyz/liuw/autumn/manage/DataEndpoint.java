package xyz.liuw.autumn.manage;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import xyz.liuw.autumn.data.DataLoader;
import xyz.liuw.autumn.data.DataSource;
import xyz.liuw.autumn.util.WebUtil;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * @author liuwei
 * Created by liuwei on 2018/12/7.
 */
// 我不需要其他管理功能，没必要使用 actuator
@RestController
public class DataEndpoint {

    @Autowired
    private DataLoader dataLoader;

    @Autowired
    private DataSource dataSource;

    @GetMapping("/manage/data")
    public DataStat getDataStat(HttpServletRequest request, HttpServletResponse response) {
        if (!checkAuth(request, response)) {
            return null;
        }

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

    // curl --silent -X POST http://localhost:8001/manage/data
    @PostMapping("/manage/data")
    public String reload(HttpServletRequest request, HttpServletResponse response) {
        if (!checkAuth(request, response)) {
            return null;
        }

        long start = System.currentTimeMillis();
        dataLoader.load();
        long cost = System.currentTimeMillis() - start;
        return String.format("Reloaded in %s ms. %s\n", cost, dataSource);
    }

    private boolean checkAuth(HttpServletRequest request, HttpServletResponse response) {
        boolean allow = "127.0.0.1".equals(WebUtil.getClientIpAddress(request));
        if (!allow) {
            try {
                response.sendError(403);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return allow;
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
