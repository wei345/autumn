package io.liuwei.autumn.controller;

import io.liuwei.autumn.ArticleService;
import io.liuwei.autumn.annotation.AccessLevel;
import io.liuwei.autumn.annotation.CheckModified;
import io.liuwei.autumn.config.AutumnProperties;
import io.liuwei.autumn.enums.AccessLevelEnum;
import io.liuwei.autumn.model.Article;
import io.liuwei.autumn.model.ArticleVO;
import io.liuwei.autumn.model.Media;
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
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.context.request.WebRequest;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
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

    private static final String TREE_JS_PATH = "/tree.json";

    @Autowired
    private ArticleService articleService;

    @Autowired
    private AutumnProperties autumnProperties;

    @Autowired
    private StaticService staticService;

    @Autowired
    private WebUtil webUtil;

    @GetMapping("")
    @ResponseBody
    public String index() {
        return "welcome";
    }

    @GetMapping(value = "/js*/all.js", produces = "text/javascript;charset=UTF-8")
    @ResponseBody
    @CheckModified
    public Object getAllJs() {
        return staticService.getJsCache();
    }

    @GetMapping(value = "/css*/all.css", produces = "text/css;charset=UTF-8")
    @ResponseBody
    @CheckModified
    public Object getAllCss() {
        return staticService.getCssCache();
    }

    @GetMapping(value = TREE_JS_PATH, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseBody
    @CheckModified
    public Object getTreeJson(@AccessLevel AccessLevelEnum accessLevel) {
        return articleService.getTreeJson(accessLevel);
    }

    @GetMapping(value = "/**/*.*")
    @ResponseBody
    public void getMedia(HttpServletRequest request, HttpServletResponse response, WebRequest webRequest,
                        @AccessLevel AccessLevelEnum accessLevel) throws IOException {
        String path = WebUtil.getInternalPath(request);
        Media media = articleService.getMedia(path);

        if (media == null || !media.getAccessLevel().allow(accessLevel)) {
            response.sendError(404);
            return;
        }

        if (webRequest.checkNotModified(media.getFile().lastModified())) {
            return;
        }

        MediaType mediaType = MimeTypeUtil.getMediaType(media.getFile().getName());
        response.setContentType(mediaType.toString());
        OutputStream out = response.getOutputStream();
        try (FileInputStream in = new FileInputStream(media.getFile())) {
            IOUtils.copy(in, out);
        }
        out.flush();
    }

    @GetMapping(value = "/**")
    public String getArticle(HttpServletRequest request, Map<String, Object> model,
                             HttpServletResponse response, @AccessLevel AccessLevelEnum accessLevel) throws IOException {
        String path = WebUtil.getInternalPath(request);
        Article article = articleService.getArticle(path);

        if (article == null || !article.getAccessLevel().allow(accessLevel)) {
            response.sendError(404);
            return null;
        }

        ArticleVO articleVO = articleService.toVO(article);
        model.put("article", articleVO);
        setGlobalConfig(model, accessLevel);
        return "article";
    }

    private void setGlobalConfig(Map<String, Object> model, AccessLevelEnum accessLevel) {
        String ctx = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest().getContextPath();
        model.put("highlightjsVersion", autumnProperties.getCodeBlock().getHighlightjsVersion());
        model.put("ctx", ctx);
        model.put("prefix", webUtil.getPrefix());
        model.put("treeJsUrl", ctx + TREE_JS_PATH + "?" + articleService.getTreeJson(accessLevel).getVersionKeyValue());
        model.put("title", autumnProperties.getSiteTitle());
    }

}
