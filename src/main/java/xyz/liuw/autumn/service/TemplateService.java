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

    private static final String FAVICON_ICO_MEDIA_PATH = "/favicon.ico";
    private static final String JS_VERSION_KEY_VALUE = "jsVersionKeyValue";
    private static final String CSS_VERSION_KEY_VALUE = "cssVersionKeyValue";
    private static final String TREE_VERSION_KEY_VALUE = "treeVersionKeyValue";
    private static final String FAVICON_URL = "faviconUrl";
    private static final String LOGGED = "logged";
    private static final String CTX = "ctx";
    private static final String PREFIX = "prefix";
    private static final String GOOGLE_ANALYTICS_ID = "googleAnalyticsId";
    private static final String TITLE = "title";
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
    private String title;

    private String faviconUrl;

    private volatile long templateLastChanged;

    private ThreadLocal<StringWriter> stringWriterThreadLocal = ThreadLocal.withInitial(() -> new StringWriter(10240));

    @PostConstruct
    private void init() {
        faviconUrl = webUtil.getContextPath() + FAVICON_ICO_MEDIA_PATH + "?" + dataService.getMediaVersionKeyValue(FAVICON_ICO_MEDIA_PATH);

        dataLoader.addTreeJsonChangedListener(this::refreshTemplateLastChanged);
        dataLoader.addMediaChangedListeners(this::refreshTemplateLastChanged);
        resourceLoader.addTemplateLastChangedListener(this::refreshTemplateLastChanged);
        staticService.addStaticChangedListener(this::refreshTemplateLastChanged);
        refreshTemplateLastChanged();
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
        model.put(CSS_VERSION_KEY_VALUE, staticService.getCssCache().getVersionKeyValue());
        model.put(JS_VERSION_KEY_VALUE, staticService.getJsCache().getVersionKeyValue());
        model.put(TREE_VERSION_KEY_VALUE, dataService.getTreeJson().getVersionKeyValue());
        model.put(FAVICON_URL, faviconUrl);
        model.put(LOGGED, isLogged());
        model.put(GOOGLE_ANALYTICS_ID, googleAnalyticsId);
        model.put(TITLE, title);
        model.put(PREFIX, webUtil.getPrefix());
    }

    /**
     * 设置 error template 需要的变量。
     * error template 收不到 model 属性，可以收到 request 属性。
     */
    public void setCtx(HttpServletRequest request) {
        request.setAttribute(CTX, webUtil.getContextPath());
        request.setAttribute(CSS_VERSION_KEY_VALUE, staticService.getCssCache().getVersionKeyValue());
        request.setAttribute(JS_VERSION_KEY_VALUE, staticService.getJsCache().getVersionKeyValue());
        request.setAttribute(TREE_VERSION_KEY_VALUE, dataService.getTreeJson().getVersionKeyValue());
        request.setAttribute(FAVICON_URL, faviconUrl);
        request.setAttribute(LOGGED, isLogged());
        request.setAttribute(GOOGLE_ANALYTICS_ID, googleAnalyticsId);
        request.setAttribute(TITLE, title);
        request.setAttribute(PREFIX, webUtil.getPrefix());
    }

    private void refreshTemplateLastChanged() {
        templateLastChanged = System.currentTimeMillis();
        logger.info("Refreshed templateLastChanged");
    }

    long getTemplateLastChanged() {
        return templateLastChanged;
    }
}
