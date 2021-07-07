package io.liuwei.autumn.controller;

import io.liuwei.autumn.ArticleService;
import io.liuwei.autumn.config.AutumnProperties;
import io.liuwei.autumn.domain.TreeJson;
import io.liuwei.autumn.model.Article;
import io.liuwei.autumn.service.DataService;
import io.liuwei.autumn.service.StaticService;
import io.liuwei.autumn.util.MimeTypeUtil;
import io.liuwei.autumn.util.WebUtil;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.context.request.WebRequest;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
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
    private DataService dataService;

    @Autowired
    private WebUtil webUtil;

    @GetMapping(value = "/js*/all.js", produces = "text/javascript;charset=UTF-8")
    @ResponseBody
    public byte[] getAllJs() {
        return staticService.getJsCache().getContent();
    }

    @GetMapping(value = "/css*/all.css", produces = "text/css;charset=UTF-8")
    @ResponseBody
    public byte[] getAllCss(WebRequest webRequest) {
        return staticService.getCssCache().getContent();
    }

    @RequestMapping(value = "/tree.json", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public String treeJson(WebRequest webRequest) {
        TreeJson treeJson = dataService.getTreeJson();

        if (WebUtil.checkNotModified(webRequest, treeJson.getEtag())) {
            return null;
        }

        return treeJson.getJson();
    }

    @GetMapping(value = "/**/*.*")
    public ResponseEntity<byte[]> getFile(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String path = WebUtil.getInternalPath(request);
        File file = articleService.getFile(path);
        if (file == null) {
            response.sendError(404);
            return null;
        }
        MediaType mediaType = MimeTypeUtil.getMediaType(path);
        byte[] content = FileUtils.readFileToByteArray(file);
        return ResponseEntity
                .status(HttpStatus.OK)
                .contentType(mediaType)
                .body(content);
    }

    @GetMapping(value = "/**")
    public String getArticle(HttpServletRequest request, Map<String, Object> model) {
        String path = WebUtil.getInternalPath(request);
        Article article = articleService.getArticle(path);
        model.put("article", article);
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
