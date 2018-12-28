package xyz.liuw.autumn.service;

import freemarker.template.Configuration;
import freemarker.template.Template;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.freemarker.FreeMarkerProperties;
import org.springframework.stereotype.Component;
import xyz.liuw.autumn.data.DataLoader;
import xyz.liuw.autumn.data.ResourceLoader;
import xyz.liuw.autumn.util.WebUtil;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
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
    private static final String TREE_VERSION = "treeVersion";
    private static final String LOGGED = "logged";
    private static final String CTX = "ctx";
    private static final String GOOGLE_ANALYTICS_ID = "googleAnalyticsId";
    private static final String SITE_TITLE = "siteTitle";
    private static Logger logger = LoggerFactory.getLogger(TemplateService.class);
    @Autowired
    private Configuration freeMarkerConfiguration;

    @Autowired
    private FreeMarkerProperties freeMarkerProperties;

    @Autowired
    private WebUtil webUtil;

    @Autowired
    private StaticService staticService;

    @Autowired
    private DataService dataService;

    @Autowired
    private DataLoader dataLoader;

    @Autowired
    private ResourceLoader resourceLoader;

    @Value("${autumn.google-analytics-id}")
    private String googleAnalyticsId;

    @Value("${autumn.title}")
    private String siteTitle;

    private volatile long templateLastModified;

    private ThreadLocal<StringWriter> stringWriterThreadLocal = ThreadLocal.withInitial(() -> new StringWriter(10240));

    @PostConstruct
    private void init() {
        dataLoader.addTreeJsonChangedListener(this::refreshTemplateLastModified);
        resourceLoader.addTemplateLastChangedListener(this::refreshTemplateLastModified);
        staticService.addStaticChangedListener(this::refreshTemplateLastModified);
        refreshTemplateLastModified();
    }

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
        model.put(CSS_CACHE_VERSION, staticService.getCssCache().getVersion());
        model.put(JS_CACHE_VERSION, staticService.getJsCache().getVersion());
        model.put(TREE_VERSION, dataService.getTreeJson().getVersion());
        model.put(LOGGED, isLogged());
        model.put(GOOGLE_ANALYTICS_ID, googleAnalyticsId);
        model.put(SITE_TITLE, siteTitle);
    }

    /**
     * 设置 error template 需要的变量。
     * error template 收不到 model 属性，可以收到 request 属性。
     */
    public void setCtx(HttpServletRequest request) {
        request.setAttribute(CTX, webUtil.getContextPath());
        request.setAttribute(CSS_CACHE_VERSION, staticService.getCssCache().getVersion());
        request.setAttribute(JS_CACHE_VERSION, staticService.getJsCache().getVersion());
        request.setAttribute(TREE_VERSION, dataService.getTreeJson().getVersion());
        request.setAttribute(LOGGED, isLogged());
        request.setAttribute(GOOGLE_ANALYTICS_ID, googleAnalyticsId);
        request.setAttribute(SITE_TITLE, siteTitle);
    }

    private void refreshTemplateLastModified() {
        templateLastModified = System.currentTimeMillis();
        logger.info("Refreshed templateLastModified");
    }

    long getTemplateLastModified() {
        return templateLastModified;
    }
}
