package xyz.liuw.autumn.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.DigestUtils;
import org.springframework.web.context.request.WebRequest;
import xyz.liuw.autumn.data.Page;
import xyz.liuw.autumn.util.WebUtil;

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

    private static final String PAGE_HTML = "pageHtml";
    private static final String TITLE = "title";
    private static Logger logger = LoggerFactory.getLogger(PageService.class);
    @Autowired
    private DataService dataService;

    @Autowired
    private MarkdownParser markdownParser;

    @Autowired
    private ResourceService resourceService;

    @Autowired
    private TemplateService templateService;

    @Autowired
    private SearchService searchService;

    @Autowired
    private WebUtil webUtil;

    public byte[] getPageContent(@NotNull Page page, Map<String, Object> model, String view, WebRequest webRequest) {
        Page.ViewCache viewCache = dataService.getViewCache(page);
        long viewLastModified = resourceService.getTemplateLastModified();
        if (viewCache == null || viewCache.getTemplateLastModified() < viewLastModified) {
            //noinspection SynchronizationOnLocalVariableOrMethodParameter
            synchronized (page) {
                viewCache = dataService.getViewCache(page);
                if (viewCache == null || viewCache.getTemplateLastModified() < viewLastModified) {
                    logger.info("Building cache path={}", page.getPath());
                    model.put(TITLE, htmlEscape(page.getTitle()));
                    model.put(PAGE_HTML, getPageHtml(page));
                    byte[] content = templateService.merge(model, view).getBytes(StandardCharsets.UTF_8);
                    String md5 = DigestUtils.md5DigestAsHex(content);
                    dataService.setViewCache(page, new Page.ViewCache(content, webUtil.getEtag(md5), viewLastModified));
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
        String html = getPageHtml(page);
        html = searchService.highlightSearchStr(html, searchStrList);
        model.put(TITLE, htmlEscape(page.getTitle()));
        model.put(PAGE_HTML, html);
        return templateService.merge(model, view);
    }

    public String getPageSource(@NotNull Page page, WebRequest webRequest) {
        // check ETag
        String md5 = page.getSourceMd5();
        if (md5 == null) {
            //noinspection SynchronizationOnLocalVariableOrMethodParameter
            synchronized (page) {
                if (page.getSourceMd5() == null) {
                    page.setSourceMd5(
                            DigestUtils.md5DigestAsHex(page.getSource().getBytes(StandardCharsets.UTF_8)));
                }
            }
            md5 = page.getSourceMd5();
        }
        String etag = webUtil.getEtag(md5);
        if (webRequest.checkNotModified(etag)) {
            return null;
        }

        return page.getSource();
    }

    private String getPageHtml(Page page) {
        if (page.getHtml() == null) {
            //noinspection SynchronizationOnLocalVariableOrMethodParameter
            synchronized (page) {
                if (page.getHtml() == null) {
                    String html = markdownParser.render(page.getTitle(), page.getBody());
                    page.setHtml(html);
                }
            }
        }
        return page.getHtml();
    }

}
