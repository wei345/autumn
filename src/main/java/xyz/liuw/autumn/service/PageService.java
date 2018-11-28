package xyz.liuw.autumn.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.DigestUtils;
import org.springframework.web.context.request.WebRequest;
import xyz.liuw.autumn.data.Page;

import javax.validation.constraints.NotNull;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.springframework.web.util.HtmlUtils.htmlEscape;

/**
 * @author liuwei
 * Created by liuwei on 2018/11/26.
 */
@Component
public class PageService {

    private static Logger logger = LoggerFactory.getLogger(PageService.class);

    @Autowired
    private DataService dataService;

    @Autowired
    private MarkdownParser markdownParser;

    @Autowired
    private TemplateWatcher templateWatcher;

    @Value("${autumn.etag.version}")
    private int etagVersion;

    @Autowired
    private TemplateService templateService;

    @Autowired
    private SearchService searchService;

    public byte[] output(@NotNull Page page, Map<String, Object> model, String view, WebRequest webRequest) {
        Page.ViewCache viewCache = dataService.getViewCache(page);
        long viewLastModified = templateWatcher.getTemplateLastModified(view);
        if (viewCache == null || viewCache.getTemplateLastModified() < viewLastModified) {
            //noinspection SynchronizationOnLocalVariableOrMethodParameter
            synchronized (page) {
                viewCache = dataService.getViewCache(page);
                if (viewCache == null || viewCache.getTemplateLastModified() < viewLastModified) {
                    logger.info("Building cache path={}", page.getPath());
                    model.put("title", htmlEscape(page.getTitle()));
                    model.put("body", getPageBodyHtml(page));
                    byte[] content = templateService.merge(model, view).getBytes(StandardCharsets.UTF_8);
                    String md5 = DigestUtils.md5DigestAsHex(content);
                    dataService.setViewCache(page, new Page.ViewCache(content, getEtag(md5), viewLastModified));
                }
            }
            viewCache = dataService.getViewCache(page);
        }

        if (webRequest.checkNotModified(viewCache.getEtag())) {
            return null;
        }

        return viewCache.getContent();
    }

    public String highlightOutput(@NotNull Page page, List<String> searchStrList, Map<String, Object> model, String view) {
        String title = htmlEscape(page.getTitle());
        title = searchService.highlightSearchStr(title, searchStrList);
        String bodyHtml = getPageBodyHtml(page);
        bodyHtml = searchService.highlightSearchStr(bodyHtml, searchStrList);
        model.put("title", title);
        model.put("body", bodyHtml);
        return templateService.merge(model, view);
    }

    private String getPageBodyHtml(Page page) {
        if (page.getBodyHtml() == null) {
            //noinspection SynchronizationOnLocalVariableOrMethodParameter
            synchronized (page) {
                if (page.getBodyHtml() == null) {
                    String html = markdownParser.render(page.getBody());
                    page.setBodyHtml(html);
                }
            }
        }
        return page.getBodyHtml();
    }

    private String getEtag(String md5) {
        return "\"" + etagVersion + "|" + md5 + "\"";
    }

}
