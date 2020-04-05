package io.liuwei.autumn.service;

import io.liuwei.autumn.data.DataLoader;
import io.liuwei.autumn.data.Page;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.DigestUtils;
import org.springframework.web.context.request.WebRequest;
import io.liuwei.autumn.util.WebUtil;

import javax.servlet.http.HttpServletRequest;
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

    private static final String TOC = "toc";
    private static final String PAGE_CONTENT = "pageContent";
    private static final String PAGE_TITLE = "pageTitle";
    private static final String PAGE_TITLE_H1 = "pageTitleH1";
    private static final String BREADCRUMB = "breadcrumb";
    private static final String PATH = "path";
    private static Logger logger = LoggerFactory.getLogger(PageService.class);
    @Autowired
    private DataService dataService;

    @Autowired
    private MarkdownParser markdownParser;

    @Autowired
    private TemplateService templateService;

    @Autowired
    private SearchService searchService;

    @Autowired
    private DataLoader dataLoader;

    @Value("${autumn.breadcrumb.enabled}")
    private boolean breadcrumbEnabled;

    public byte[] handlePageRequest(@NotNull Page page,
                                    Map<String, Object> model,
                                    String view,
                                    WebRequest webRequest,
                                    HttpServletRequest request) {
        Page.ViewCache viewCache = dataService.getViewCache(page);
        long templateLastModified = templateService.getTemplateLastChanged();
        if (viewCache == null || viewCache.getTime() < templateLastModified) {
            //noinspection SynchronizationOnLocalVariableOrMethodParameter
            synchronized (page) {
                viewCache = dataService.getViewCache(page);
                if (viewCache == null || viewCache.getTime() < templateLastModified) {
                    logger.info("Building cache path={}", page.getPath());
                    Page.PageHtml pageHtml = getPageHtml(page, WebUtil.getInternalPath(request));
                    model.put(PAGE_TITLE, htmlEscape(page.getTitle()));
                    model.put(PAGE_TITLE_H1, pageHtml.getTitle());
                    model.put(TOC, pageHtml.getToc());
                    model.put(PAGE_CONTENT, pageHtml.getContent());
                    addPageMetaProperties(model, page);
                    byte[] content = templateService.merge(model, view).getBytes(StandardCharsets.UTF_8);
                    String md5 = DigestUtils.md5DigestAsHex(content);
                    String etag = WebUtil.getEtag(md5);
                    dataService.setViewCache(page, new Page.ViewCache(content, etag));
                }
            }
            viewCache = dataService.getViewCache(page);
        }

        if (WebUtil.checkNotModified(webRequest, viewCache.getEtag())) {
            return null;
        }

        return viewCache.getContent();
    }

    public String handlePageHighlightRequest(@NotNull Page page,
                                             List<String> searchStrList,
                                             Map<String, Object> model,
                                             String view,
                                             HttpServletRequest request) {
        Page.PageHtml pageHtml = getPageHtml(page, WebUtil.getInternalPath(request));
        String toc = searchService.highlightSearchStr(pageHtml.getToc(), searchStrList);
        String content = searchService.highlightSearchStr(pageHtml.getContent(), searchStrList);
        String title = searchService.highlightSearchStr(pageHtml.getTitle(), searchStrList);

        model.put(PAGE_TITLE, htmlEscape(page.getTitle()));
        model.put(PAGE_TITLE_H1, title);
        model.put(TOC, toc);
        model.put(PAGE_CONTENT, content);
        addPageMetaProperties(model, page);
        return templateService.merge(model, view);
    }

    private void addPageMetaProperties(Map<String, Object> model, Page page){
        if (breadcrumbEnabled) {
            model.put(BREADCRUMB, dataService.getBreadcrumbLinks(page));
        }
        model.put(PATH, page.getPath());
    }

    public String handlePageSourceRequest(@NotNull Page page, WebRequest webRequest) {
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
        String etag = WebUtil.getEtag(md5);
        if (WebUtil.checkNotModified(webRequest, etag)) {
            return null;
        }

        return page.getSource();
    }

    private Page.PageHtml getPageHtml(Page page, String path) {
        if (page.getPageHtml() == null || page.getPageHtml().getTime() < dataLoader.getMediaLastChanged()) {
            //noinspection SynchronizationOnLocalVariableOrMethodParameter
            synchronized (page) {
                if (page.getPageHtml() == null || page.getPageHtml().getTime() < dataLoader.getMediaLastChanged()) {
                    Page.PageHtml pageHtml = markdownParser.render(page.getTitle(), page.getBody(), path);
                    page.setPageHtml(pageHtml);
                }
            }
        }
        return page.getPageHtml();
    }

}