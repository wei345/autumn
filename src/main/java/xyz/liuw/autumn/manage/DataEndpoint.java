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
    public String info(HttpServletRequest request, HttpServletResponse response) {
        if (!checkAuth(request, response)) {
            return null;
        }

        return dataSource.toString();
    }

    // curl --silent -X POST http://localhost:${server.port}${server.servlet.context-path}/manage/data
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

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
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
}
