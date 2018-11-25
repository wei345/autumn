package xyz.liuw.autumn.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import xyz.liuw.autumn.data.DataSource;
import xyz.liuw.autumn.data.Media;
import xyz.liuw.autumn.data.Page;
import xyz.liuw.autumn.data.TreeJson;

/**
 * @author liuwei
 * Created by liuwei on 2018/11/23.
 */
@Component
public class DataService {

    @Autowired
    private DataSource dataSource;

    public TreeJson getTreeJson() {
        return SecurityService.isLogged() ?
                dataSource.getAllData().getTreeJson() :
                dataSource.getPublishedData().getTreeJson();
    }

    public Page getPage(String path) {
        return SecurityService.isLogged() ?
                dataSource.getAllData().getPageMap().get(path) :
                dataSource.getPublishedData().getPageMap().get(path);
    }

    public Media getMedia(String path) {
        return SecurityService.isLogged() ?
                dataSource.getAllData().getMediaMap().get(path) :
                dataSource.getPublishedData().getMediaMap().get(path);
    }

    /**
     * 返回 page 是否存在，跳过权限检查
     */
    public boolean pageExist(String path) {
        return dataSource.getAllData().getPageMap().containsKey(path);
    }
}
