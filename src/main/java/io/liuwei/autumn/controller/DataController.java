package io.liuwei.autumn.controller;

import io.liuwei.autumn.manager.MediaManager;
import io.liuwei.autumn.model.DataInfo;
import io.liuwei.autumn.util.WebUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 提供重新加载数据接口
 *
 * @author liuwei
 * Created by liuwei on 2018/12/7.
 */
// 我不需要其他管理功能，没必要使用 actuator
@RestController
public class DataController {

    @Autowired
    private MediaManager mediaManager;

    @GetMapping("/data/info")
    public DataInfo getDataInfo(HttpServletRequest request, HttpServletResponse response) {
        if (!checkAuth(request, response)) {
            return null;
        }
        return mediaManager.getDataInfo();
    }

    // curl --silent -X POST http://localhost:8061/data/reload
    @PostMapping("/data/reload")
    public DataInfo reload(HttpServletRequest request, HttpServletResponse response) {
        if (!checkAuth(request, response)) {
            return null;
        }

        return mediaManager.reload();
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
