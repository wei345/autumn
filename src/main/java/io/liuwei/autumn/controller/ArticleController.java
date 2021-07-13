package io.liuwei.autumn.controller;

import io.liuwei.autumn.annotation.CheckModified;
import io.liuwei.autumn.component.MediaRevisionResolver;
import io.liuwei.autumn.constant.Constants;
import io.liuwei.autumn.enums.AccessLevelEnum;
import io.liuwei.autumn.model.Article;
import io.liuwei.autumn.model.ArticleVO;
import io.liuwei.autumn.model.Media;
import io.liuwei.autumn.model.RevisionEtag;
import io.liuwei.autumn.service.ArticleService;
import io.liuwei.autumn.service.SearchService;
import io.liuwei.autumn.util.WebUtil;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.context.request.ServletWebRequest;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Comparator;
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
    private SearchService searchService;

    @Autowired
    private MediaRevisionResolver mediaRevisionResolver;

    @Value("${autumn.breadcrumb.enabled:false}")
    private boolean isBreadcrumbEnabled;

    @GetMapping("")
    public String home(AccessLevelEnum accessLevel, Model model) {
        List<Article> list = articleService.listArticles(accessLevel);
        list.sort(Comparator
                .comparing(Article::getModified).reversed()
                .thenComparing(Article::getPath));

        if (list.size() > 20) {
            list = list.subList(0, 20);
        }

        model.addAttribute("articles", list);
        return "home";
    }

    @GetMapping(value = Constants.TREE_DOT_JSON, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseBody
    @CheckModified
    public Object getTreeJson(AccessLevelEnum accessLevel) {
        return articleService.getTreeJson(accessLevel);
    }

    @GetMapping("/sitemap")
    public String sitemap(AccessLevelEnum accessLevel, Model model) {
        String treeHtml = articleService.getTreeHtml(accessLevel);
        model.addAttribute("treeHtml", treeHtml);
        return "sitemap";
    }

    // 带扩展名，访问文件
    @GetMapping(value = "/**/*.*")
    @ResponseBody
    public ResponseEntity<byte[]> getMedia(AccessLevelEnum accessLevel, HttpServletResponse response,
                                           ServletWebRequest webRequest) throws IOException {
        String path = WebUtil.getInternalPath(webRequest.getRequest());
        Media media = articleService.getMedia(path);
        RevisionEtag re;

        if (media == null
                || !media.getAccessLevel().allow(accessLevel)
                || (re = mediaRevisionResolver.getRevision(media)) == null) {
            response.sendError(404);
            return null;
        }

        if (WebUtil.checkNotModified(re.getRevision(), re.getEtag(), webRequest)) {
            return null;
        }

        if (re.getRevisionContent() != null) {
            return ResponseEntity
                    .status(HttpStatus.OK)
                    .contentType(re.getRevisionContent().getMediaType())
                    .body(re.getRevisionContent().getContent());
        } else {
            response.setContentType(media.getMediaType().toString());
            OutputStream out = response.getOutputStream();
            try (FileInputStream in = new FileInputStream(media.getFile())) {
                IOUtils.copy(in, out);
            }
            out.flush();
            return null;
        }
    }

    // 不带扩展名，访问文章
    @GetMapping(value = "/**")
    public String getArticle(String[] h, // h=a&h=b..
                             AccessLevelEnum accessLevel,
                             HttpServletRequest request,
                             HttpServletResponse response,
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

        if (isBreadcrumbEnabled) {
            model.put("breadcrumb", articleService.getBreadcrumbLinks(article, accessLevel));
        }
        model.put("article", articleVO);
        return "article";
    }

}
