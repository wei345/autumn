package io.liuwei.autumn.service;

import io.liuwei.autumn.data.DataSource;
import io.liuwei.autumn.domain.Media;
import io.liuwei.autumn.domain.Page;
import io.liuwei.autumn.model.RevisionContent;
import io.liuwei.autumn.domain.Link;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;


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

    public RevisionContent getTreeJson() {
        return UserService.isLogged() ?
                dataSource.getAllData().getTreeJson() :
                dataSource.getPublishedData().getTreeJson();
    }

    public Media getMedia(String path) {
        if (UserService.isLogged()) {
            return dataSource.getAllData().getPath2media().get(path);
        }

        Media media = dataSource.getPublishedData().getPath2media().get(path);
        if (media != null) {
            return media;
        }

        if (dataSource.getAllData().getPath2media().containsKey(path)) {
            return LOGIN_REQUIRED_MEDIA;
        }
        return null;
    }

    public String getMediaVersionKeyValue(String path) {
        Media media = dataSource.getAllData().getPath2media().get(path);
        if (media != null) {
            return media.getVersionKeyValue();
        }
        return "";
    }

    Page.ViewCache getViewCache(Page page) {
        return UserService.isLogged() ? page.getUserViewCache() : page.getGuestViewCache();
    }

    void setViewCache(Page page, Page.ViewCache viewCache) {
        if (UserService.isLogged()) {
            page.setUserViewCache(viewCache);
        } else {
            page.setGuestViewCache(viewCache);
        }
    }

    Map<String, Page> getPageMap() {
        if (UserService.isLogged()) {
            return dataSource.getAllData().getPath2page();
        } else {
            return dataSource.getPublishedData().getPath2page();
        }
    }

    /**
     * @param path 首页 /，其他 /xxx
     * @return null 未找到, LOGIN_REQUIRED_PAGE 找到了但登录后才能看, Page 找到
     */
    public Page getPage(String path) {
        Page page;

        if ("/".equals(path)) {
            if (UserService.isLogged()) {
                return dataSource.getAllData().getHomepage();
            } else {
                return dataSource.getPublishedData().getHomepage();
            }
        }

        if ("/help".equals(path)) {
            return staticService.getHelpPage();
        }

        if (UserService.isLogged()) {
            return dataSource.getAllData().getPath2page().get(path);
        }

        page = dataSource.getPublishedData().getPath2page().get(path);
        if (page != null) {
            return page;
        }

        if (dataSource.getAllData().getPath2page().containsKey(path)) {
            return LOGIN_REQUIRED_PAGE;
        }

        return null;
    }

    List<Link> getBreadcrumbLinks(Page page) {
        Assert.notNull(page, "page is null");

        LinkedList<Link> links = new LinkedList<>();
        String path = page.getPath();
        int lastSlash;
        while ((lastSlash = path.lastIndexOf('/')) >= 0) {
            String parent = path.substring(0, lastSlash);
            links.addFirst(getBreadcrumbDirectoryLink(parent));
            path = parent;
        }

        if (page.getPath().equals(links.getLast().getHref())) { // e.g. page.path is / or /a/b/index or /a/b/b
            links.getLast().setHref(null);
        } else {
            links.addLast(new Link(page.getTitle()));
        }

        return links;
    }

    /**
     * @param path e.g. /a/b
     */
    private Link getBreadcrumbDirectoryLink(String path) {
        DataSource.Data data = UserService.isLogged() ? dataSource.getAllData() : dataSource.getPublishedData();
        Map<String, Page> pageMap = data.getPath2page();

        Page page = path.length() == 0 ?
                data.getHomepage() :
                pageMap.get(path);

        // 查找 /path/index
        if (page == null) {
            page = pageMap.get(path + "/index");
        }

        // 查找 /path/name
        String name = null;
        if (page == null) {
            name = path.substring(path.lastIndexOf('/') + 1);
            page = pageMap.get(path + "/" + name);
        }

        return page == null ?
                new Link(name) :
                new Link(page.getTitle(), page.getPath());
    }
}
