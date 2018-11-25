package xyz.liuw.autumn.service;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.freemarker.FreeMarkerProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.DigestUtils;
import org.springframework.web.context.request.WebRequest;
import xyz.liuw.autumn.data.Page;

import javax.validation.constraints.NotNull;
import java.io.IOException;
import java.io.StringWriter;
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
    private Configuration freeMarkerConfiguration;
    @Autowired
    private FreeMarkerProperties freeMarkerProperties;

    private ThreadLocal<StringWriter> stringWriterThreadLocal = ThreadLocal.withInitial(() -> new StringWriter(10240));


    public byte[] output(@NotNull Page page, Map<String, Object> model, String view, WebRequest webRequest) throws IOException, TemplateException {
        Page.ViewCache viewCache = dataService.getViewCache(page);
        if (viewCache == null) {
            //noinspection SynchronizationOnLocalVariableOrMethodParameter
            synchronized (page) {
                if (dataService.getViewCache(page) == null) {
                    logger.info("Building cache path={}", page.getPath());
                    if (page.getBodyHtml() == null) {
                        String html = markdownParser.render(page.getBody());
                        page.setBodyHtml(html);
                    }

                    model.put("title", page.getTitle());
                    model.put("body", page.getBodyHtml());
                    Template template = freeMarkerConfiguration.getTemplate(view + freeMarkerProperties.getSuffix());
                    StringWriter out = stringWriterThreadLocal.get();
                    template.process(model, out);

                    byte[] content = out.toString().getBytes(StandardCharsets.UTF_8);
                    out.getBuffer().setLength(0);
                    String md5 = DigestUtils.md5DigestAsHex(content);
                    String etag = "\"" + md5 + "\"";
                    dataService.setViewCache(page, new Page.ViewCache(content, etag));
                }
            }
            viewCache = dataService.getViewCache(page);
        }

        if (webRequest.checkNotModified(viewCache.getEtag())) {
            return null;
        }

        return viewCache.getContent();
    }

}
