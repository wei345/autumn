package xyz.liuw.autumn.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static Logger logger = LoggerFactory.getLogger(DataService.class);

    @Autowired
    private DataSource dataSource;

    public TreeJson getTreeJson() {
        return SecurityService.isLogged() ?
                dataSource.getAllData().getTreeJson() :
                dataSource.getPublishedData().getTreeJson();
    }

    public Media getMedia(String path) {
        return SecurityService.isLogged() ?
                dataSource.getAllData().getMediaMap().get(path) :
                dataSource.getPublishedData().getMediaMap().get(path);
    }

    public Page.ViewCache getViewCache(Page page) {
        return SecurityService.isLogged() ? page.getUserViewCache() : page.getGuestViewCache();
    }

    public Page getPage(String path) {
        return SecurityService.isLogged() ?
                dataSource.getAllData().getPageMap().get(path) :
                dataSource.getPublishedData().getPageMap().get(path);
    }

    public void setViewCache(Page page, Page.ViewCache viewCache) {
        if (SecurityService.isLogged()) {
            page.setUserViewCache(viewCache);
        } else {
            page.setGuestViewCache(viewCache);
        }
    }

    /**
     * 返回 page 是否存在，跳过权限检查
     */
    public boolean pageExist(String path) {
        return dataSource.getAllData().getPageMap().containsKey(path);
    }
}
