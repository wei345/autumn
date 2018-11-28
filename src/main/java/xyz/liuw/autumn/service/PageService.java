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
import java.util.Map;

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

    public byte[] output(@NotNull Page page, Map<String, Object> model, String view, WebRequest webRequest) {
        Page.ViewCache viewCache = dataService.getViewCache(page);
        long viewLastModified = templateWatcher.getTemplateLastModified(view);
        if (viewCache == null || viewCache.getTemplateLastModified() < viewLastModified) {
            //noinspection SynchronizationOnLocalVariableOrMethodParameter
            synchronized (page) {
                viewCache = dataService.getViewCache(page);
                if (viewCache == null || viewCache.getTemplateLastModified() < viewLastModified) {
                    logger.info("Building cache path={}", page.getPath());
                    if (page.getBodyHtml() == null) {
                        String html = markdownParser.render(page.getBody());
                        page.setBodyHtml(html);
                    }

                    model.put("title", page.getTitle());
                    model.put("body", page.getBodyHtml());
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

    private String getEtag(String md5) {
        return "\"" + etagVersion + "|" + md5 + "\"";
    }

}
