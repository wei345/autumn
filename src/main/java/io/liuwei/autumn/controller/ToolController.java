package io.liuwei.autumn.controller;

import io.liuwei.autumn.util.WebUtil;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;

/**
 * @author liuwei602099
 * @since 2020-09-14 17:21
 */
@Controller
public class ToolController {

    @GetMapping("/myip")
    @ResponseBody
    public String myIp(HttpServletRequest request) {
        return WebUtil.getClientIpAddress(request);
    }

}
