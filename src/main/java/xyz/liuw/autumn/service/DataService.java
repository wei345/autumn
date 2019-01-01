package xyz.liuw.autumn.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import xyz.liuw.autumn.data.DataSource;
import xyz.liuw.autumn.data.Media;
import xyz.liuw.autumn.data.Page;
import xyz.liuw.autumn.data.TreeJson;

import javax.annotation.PostConstruct;
import java.util.Map;

import static xyz.liuw.autumn.service.UserService.isLogged;


/**
 * @author liuwei
 * Created by liuwei on 2018/11/23.
 */
@Component
public class DataService {

    public static final Page LOGIN_REQUIRED_PAGE = new Page();

    public static final Media LOGIN_REQUIRED_MEDIA = new Media();

    @Autowired
    private DataSource dataSource;

    @Autowired
    private StaticService staticService;


    private volatile long mediaLastChanged;

    @PostConstruct
    private void init(){

    }

    private void refreshMediaLastChanged(){
        mediaLastChanged = System.currentTimeMillis();
    }

    public TreeJson getTreeJson() {
        return isLogged() ?
                dataSource.getAllData().getTreeJson() :
                dataSource.getPublishedData().getTreeJson();
    }

    public Media getMedia(String path) {
        if (isLogged()) {
            return dataSource.getAllData().getMediaMap().get(path);
        }

        Media media = dataSource.getPublishedData().getMediaMap().get(path);
        if (media != null) {
            return media;
        }

        if (dataSource.getAllData().getMediaMap().containsKey(path)) {
            return LOGIN_REQUIRED_MEDIA;
        }
        return null;
    }

    public String getMediaVersionKeyValue(String path) {
        Media media = dataSource.getAllData().getMediaMap().get(path);
        if (media != null) {
            return media.getVersionKeyValue();
        }
        return "";
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
     * @return null 未找到, LOGIN_REQUIRED_PAGE 找到了但登录后才能看, Page 找到
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
            return staticService.getHelpPage();
        }

        if (isLogged()) {
            return dataSource.getAllData().getPageMap().get(path);
        }

        page = dataSource.getPublishedData().getPageMap().get(path);
        if (page != null) {
            return page;
        }

        if (dataSource.getAllData().getPageMap().containsKey(path)) {
            return LOGIN_REQUIRED_PAGE;
        }

        return null;
    }

}
