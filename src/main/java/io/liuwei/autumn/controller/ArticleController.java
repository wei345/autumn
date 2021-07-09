package io.liuwei.autumn.controller;

import io.liuwei.autumn.ArticleService;
import io.liuwei.autumn.Constants;
import io.liuwei.autumn.annotation.AccessLevel;
import io.liuwei.autumn.annotation.CheckModified;
import io.liuwei.autumn.enums.AccessLevelEnum;
import io.liuwei.autumn.model.Article;
import io.liuwei.autumn.model.ArticleVO;
import io.liuwei.autumn.model.Media;
import io.liuwei.autumn.service.SearchService;
import io.liuwei.autumn.service.StaticService;
import io.liuwei.autumn.util.MimeTypeUtil;
import io.liuwei.autumn.util.WebUtil;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.context.request.WebRequest;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.List;
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
    private StaticService staticService;

    @Autowired
    private SearchService searchService;

    @GetMapping("")
    public String index(@AccessLevel AccessLevelEnum accessLevel, Model model) {
        List<Article> list = articleService.listArticles(accessLevel);
        list.sort((o1, o2) -> {
            // 一定要分出先后，也就是不能返回 0，否则每次搜索结果顺序可能不完全一样
            int v;

            // 最近修改日期
            v = Long.compare(o2.getModified().getTime(), o1.getModified().getTime());
            if (v != 0) {
                return v;
            }

            // 字典顺序
            return o1.getPath().compareTo(o2.getPath());
        });

        if (list.size() > 20) {
            list = list.subList(0, 20);
        }

        model.addAttribute("articles", list);
        return "index";
    }

    @GetMapping(value = Constants.ALL_JS_PATH, produces = MimeTypeUtil.TEXT_JAVASCRIPT_UTF8)
    @ResponseBody
    @CheckModified
    public Object getAllJs() {
        return staticService.getJsCache();
    }

    @GetMapping(value = Constants.ALL_CSS_PATH, produces = MimeTypeUtil.TEXT_CSS_UTF8)
    @ResponseBody
    @CheckModified
    public Object getAllCss() {
        return staticService.getCssCache();
    }

    @GetMapping(value = Constants.TREE_JSON_PATH, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseBody
    @CheckModified
    public Object getTreeJson(@AccessLevel AccessLevelEnum accessLevel) {
        return articleService.getTreeJson(accessLevel);
    }

    @GetMapping("/sitemap")
    public String sitemap(@AccessLevel AccessLevelEnum accessLevel, Model model) {
        String treeHtml = articleService.getTreeHtml(accessLevel);
        model.addAttribute("treeHtml", treeHtml);
        return "sitemap";
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
    public String getArticle(String[] h, // h=a&h=b..
                             HttpServletRequest request,
                             HttpServletResponse response,
                             @AccessLevel AccessLevelEnum accessLevel,
                             Map<String, Object> model) throws IOException {
        String path = WebUtil.getInternalPath(request);
        Article article = articleService.getArticle(path);

        if (article == null || !article.getAccessLevel().allow(accessLevel)) {
            response.sendError(404);
            return null;
        }

        ArticleVO articleVO = articleService.toVO(article);

        // highlight
        if (h != null && h.length > 0) {
            String[] strings = h;
            // 太多高亮词会影响性能，正常不会太多
            if (strings.length > 10) {
                strings = new String[10];
                System.arraycopy(h, 0, strings, 0, strings.length);
            }
            List<String> searchStrList = Arrays.asList(strings);
            articleVO.setTocHtml(searchService.highlightSearchStr(articleVO.getTocHtml(), searchStrList));
            articleVO.setContentHtml(searchService.highlightSearchStr(articleVO.getContentHtml(), searchStrList));
            articleVO.setTitleHtml(searchService.highlightSearchStr(articleVO.getTitleHtml(), searchStrList));
        }

        model.put("article", articleVO);
        return "article";
    }

}
