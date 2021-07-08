package io.liuwei.autumn.controller;

import io.liuwei.autumn.ArticleService;
import io.liuwei.autumn.annotation.AccessLevel;
import io.liuwei.autumn.annotation.CheckModified;
import io.liuwei.autumn.config.AutumnProperties;
import io.liuwei.autumn.enums.AccessLevelEnum;
import io.liuwei.autumn.model.Article;
import io.liuwei.autumn.model.RevisionContent;
import io.liuwei.autumn.service.StaticService;
import io.liuwei.autumn.util.MimeTypeUtil;
import io.liuwei.autumn.util.WebUtil;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.context.request.WebRequest;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;

/**
 * @author liuwei
 * @since 2021-07-07 18:53
 */
@Controller
@RequestMapping
public class ArticleController {

    @Autowired
    private ArticleService articleService;

    @Autowired
    private AutumnProperties autumnProperties;

    @Autowired
    private StaticService staticService;

    @Autowired
    private WebUtil webUtil;

    @GetMapping(value = "/js*/all.js", produces = "text/javascript;charset=UTF-8")
    @ResponseBody
    @CheckModified
    public RevisionContent getAllJs() {
        return staticService.getJsCache();
    }

    @GetMapping(value = "/css*/all.css", produces = "text/css;charset=UTF-8")
    @ResponseBody
    @CheckModified
    public RevisionContent getAllCss() {
        return staticService.getCssCache();
    }

    @GetMapping(value = "/tree.json", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseBody
    @CheckModified
    public RevisionContent getTreeJson(@AccessLevel AccessLevelEnum accessLevel) {
        return articleService.getTreeJson(accessLevel);
    }

    @GetMapping(value = "/**/*.*")
    @ResponseBody
    public void getFile(HttpServletRequest request, HttpServletResponse response, WebRequest webRequest) throws IOException {
        String path = WebUtil.getInternalPath(request);
        File file = articleService.getFile(path);
        if (file == null) {
            response.sendError(404);
            return;
        }
        if (webRequest.checkNotModified(file.lastModified())) {
            return;
        }
        MediaType mediaType = MimeTypeUtil.getMediaType(file.getName());
        response.setContentType(mediaType.toString());
        OutputStream out = response.getOutputStream();
        try (FileInputStream in = new FileInputStream(file)) {
            IOUtils.copy(in, out);
        }
        out.flush();
    }

    @GetMapping(value = "/**")
    public String getArticle(HttpServletRequest request, Map<String, Object> model,
                             HttpServletResponse response, @AccessLevel AccessLevelEnum accessLevel) throws IOException {
        String path = WebUtil.getInternalPath(request);
        Article article = articleService.getArticle(path);
        if (!articleService.checkAccessLevel(article, accessLevel)) {
            response.sendError(404);
            return null;
        }

        model.put("article", articleService.toVO(article));
        setGlobalConfig(model);
        return "article";
    }

    private void setGlobalConfig(Map<String, Object> model) {
        model.put("highlightjsVersion", autumnProperties.getCodeBlock().getHighlightjsVersion());
        model.put("ctx", "");
        model.put("prefix", webUtil.getPrefix());
        model.put("treeVersionKeyValue", "");
        model.put("pageTitle", "pageTitle");
        model.put("title", "title");
    }

}
