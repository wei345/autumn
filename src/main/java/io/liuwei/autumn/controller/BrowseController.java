package io.liuwei.autumn.controller;

import io.liuwei.autumn.service.ProxyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.regex.Pattern;

/**
 * @author liuwei
 * @since 2022-10-27 16:14
 */
@Controller
@RequestMapping("/browse")
@RequiredArgsConstructor
public class BrowseController {
    private static final Pattern HTTP_URL_PREFIX_PATTERN = Pattern
            .compile("^https?://", Pattern.CASE_INSENSITIVE);

    private final ProxyService proxyService;

    @RequestMapping
    public ResponseEntity<?> browse(String url, HttpServletRequest request) throws IOException {

        if (HTTP_URL_PREFIX_PATTERN.matcher(url).find()) {
            return proxyService.proxy(request, url);
        }
        return null;
    }

}
