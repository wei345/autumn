package io.liuwei.autumn.controller;

import io.liuwei.autumn.annotation.CheckModified;
import io.liuwei.autumn.annotation.ViewCache;
import io.liuwei.autumn.constant.Constants;
import io.liuwei.autumn.service.StaticService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Map;

/**
 * @author liuwei
 * @since 2021-07-11 11:59
 */
@Controller
public class StaticController {

    @Autowired
    private StaticService staticService;

    @GetMapping(Constants.JS_ALL_DOT_JS)
    @ResponseBody
    @CheckModified
    public Object getAllJs() {
        return staticService.getAllJs();
    }

    @GetMapping(Constants.CSS_ALL_DOT_CSS)
    @ResponseBody
    @CheckModified
    public Object getAllCss() {
        return staticService.getAllCss();
    }

    @ViewCache
    @GetMapping(Constants.HELP)
    public Object help(Map<String, Object> model) {
        model.put("contentHtml", staticService.getHelpContent());
        return "content";
    }
}
