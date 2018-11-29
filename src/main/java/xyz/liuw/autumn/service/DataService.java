package xyz.liuw.autumn.service;

import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import xyz.liuw.autumn.data.DataSource;
import xyz.liuw.autumn.data.Media;
import xyz.liuw.autumn.data.Page;
import xyz.liuw.autumn.data.TreeJson;

import javax.validation.constraints.NotNull;
import java.util.Map;

import static xyz.liuw.autumn.service.SecurityService.isLogged;

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
        return isLogged() ?
                dataSource.getAllData().getTreeJson() :
                dataSource.getPublishedData().getTreeJson();
    }

    public Media getMedia(String path) {
        return isLogged() ?
                dataSource.getAllData().getMediaMap().get(path) :
                dataSource.getPublishedData().getMediaMap().get(path);
    }

    public Page.ViewCache getViewCache(Page page) {
        return isLogged() ? page.getUserViewCache() : page.getGuestViewCache();
    }

    public void setViewCache(Page page, Page.ViewCache viewCache) {
        if (isLogged()) {
            page.setUserViewCache(viewCache);
        } else {
            page.setGuestViewCache(viewCache);
        }
    }

    public Map<String, Page> getPageMap() {
        if (isLogged()) {
            return dataSource.getAllData().getPageMap();
        } else {
            return dataSource.getPublishedData().getPageMap();
        }
    }

    /**
     * 在显示 page 前一定要检查权限
     */
    public SecurityBox<Page> getPageSecurityBox(String path) {
        Page page = dataSource.getAllData().getPageMap().get(path);
        return page == null ? null : new SecurityBox<>(page);
    }

    /**
     * 调用者想看看有没有数据，如果有数据，检查权限，如果有权限，读取内容，如果没权限，提示用户没权限或需要登录。
     * "有没有数据"和"读取内容"，这是两步操作，有可能第二步之前另一个线程删除了数据，如果保证并发安全，那么需要
     * 加锁，成本高。如果"看看有没有数据"时把数据交给调用者，那么会有更多安全风险，万一调用者忘记检查用户权限了呢。
     * <p>
     * 用这个类包装数据，调用者可以知道有没有数据，确保会经过权限检查，也不会带来并发安全问题。
     */
    public static class SecurityBox<T> {
        private T data;

        SecurityBox(@NotNull T data) {
            Validate.notNull(data);
            this.data = data;
        }

        /**
         * @return data, null 如果当前未登录或权限不足
         */
        public T get() {
            if (isLogged()) {
                return data;
            }
            return null;
        }
    }
}
