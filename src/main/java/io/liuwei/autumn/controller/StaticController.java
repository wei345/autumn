package io.liuwei.autumn.controller;

import io.liuwei.autumn.annotation.CheckModified;
import io.liuwei.autumn.constant.Constants;
import io.liuwei.autumn.service.StaticService;
import io.liuwei.autumn.util.MimeTypeUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * @author liuwei
 * @since 2021-07-11 11:59
 */
@Controller
public class StaticController {

    @Autowired
    private StaticService staticService;

    @GetMapping(value = Constants.JS_ALL_DOT_JS, produces = MimeTypeUtil.TEXT_JAVASCRIPT_UTF8)
    @ResponseBody
    @CheckModified
    public Object getAllJs() {
        return staticService.getJsCache();
    }

    @GetMapping(value = Constants.CSS_ALL_DOT_CSS, produces = MimeTypeUtil.TEXT_CSS_UTF8)
    @ResponseBody
    @CheckModified
    public Object getAllCss() {
        return staticService.getCssCache();
    }

    @GetMapping("/help")
    public String help(Model model) {
        model.addAttribute("contentHtml", staticService.getHelpCache());
        return "content";
    }
}
