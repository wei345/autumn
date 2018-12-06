package xyz.liuw.autumn.service;

import freemarker.template.Configuration;
import freemarker.template.Template;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.freemarker.FreeMarkerProperties;
import org.springframework.stereotype.Component;
import xyz.liuw.autumn.util.WebUtil;

import java.io.StringWriter;
import java.util.Map;

import static xyz.liuw.autumn.service.UserService.isLogged;

/**
 * @author liuwei
 * Created by liuwei on 2018/11/28.
 */
@Component
public class TemplateService {

    private static final String JS_CACHE_VERSION = "jsCacheVersion";
    private static final String CSS_CACHE_VERSION = "cssCacheVersion";
    private static final String LOGGED_MODEL_KEY = "logged";
    private static final String CTX = "ctx";
    @Autowired
    private Configuration freeMarkerConfiguration;
    @Autowired
    private FreeMarkerProperties freeMarkerProperties;
    @Autowired
    private WebUtil webUtil;
    @Autowired
    private ResourceService resourceService;
    private ThreadLocal<StringWriter> stringWriterThreadLocal = ThreadLocal.withInitial(() -> new StringWriter(10240));

    public String merge(Map<String, Object> model, String view) {
        Template template;
        try {
            setCtx(model);
            template = freeMarkerConfiguration.getTemplate(view + freeMarkerProperties.getSuffix());
            StringWriter out = stringWriterThreadLocal.get();
            out.getBuffer().setLength(0);
            template.process(model, out);
            return out.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void setCtx(Map<String, Object> model) {
        model.put(CTX, webUtil.getContextPath());
        model.put(CSS_CACHE_VERSION, resourceService.getCssCache().getVersion());
        model.put(JS_CACHE_VERSION, resourceService.getJsCache().getVersion());
    }

    public void setLogged(Map<String, Object> model) {
        model.put(LOGGED_MODEL_KEY, isLogged());
    }
}
