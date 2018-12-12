package xyz.liuw.autumn.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import xyz.liuw.autumn.data.DataSource;
import xyz.liuw.autumn.data.Media;
import xyz.liuw.autumn.data.Page;
import xyz.liuw.autumn.data.TreeJson;

import java.util.Map;

import static xyz.liuw.autumn.service.UserService.isLogged;


/**
 * @author liuwei
 * Created by liuwei on 2018/11/23.
 */
@Component
public class DataService {

    public static final Page PAGE_NEED_LOGIN = new Page();

    @Autowired
    private DataSource dataSource;

    public TreeJson getTreeJson() {
        return isLogged() ?
                dataSource.getAllData().getTreeJson() :
                dataSource.getPublishedData().getTreeJson();
    }

    public Media getMedia(String path) {
        return isLogged() ?
                dataSource.getAllData().getMediaMap().get(path) :
                dataSource.getPublishedData().getMediaMap().get(path);
    }

    Page.ViewCache getViewCache(Page page) {
        return isLogged() ? page.getUserViewCache() : page.getGuestViewCache();
    }

    void setViewCache(Page page, Page.ViewCache viewCache) {
        if (isLogged()) {
            page.setUserViewCache(viewCache);
        } else {
            page.setGuestViewCache(viewCache);
        }
    }

    Map<String, Page> getPageMap() {
        if (isLogged()) {
            return dataSource.getAllData().getPageMap();
        } else {
            return dataSource.getPublishedData().getPageMap();
        }
    }

    /**
     * @param path 首页 /，其他 /xxx
     * @return null 未找到, PAGE_NEED_LOGIN 找到了但登录后才能看, Page 找到
     */
    public Page getPage(String path) {
        Page page;

        if ("/".equals(path)) {
            if (isLogged()) {
                return dataSource.getAllData().getHomepage();
            } else {
                return dataSource.getPublishedData().getHomepage();
            }
        }

        if ("/help".equals(path)) {
            if (isLogged()) {
                return dataSource.getAllData().getHelpPage();
            } else {
                return dataSource.getPublishedData().getHelpPage();
            }
        }

        if (isLogged()) {
            return dataSource.getAllData().getPageMap().get(path);
        }

        page = dataSource.getPublishedData().getPageMap().get(path);
        if (page != null) {
            return page;
        }

        if (dataSource.getAllData().getPageMap().containsKey(path)) {
            return PAGE_NEED_LOGIN;
        }

        return null;
    }

}
