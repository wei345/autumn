package xyz.liuw.autumn.service;

import freemarker.template.Configuration;
import freemarker.template.Template;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.freemarker.FreeMarkerProperties;
import org.springframework.stereotype.Component;

import java.io.StringWriter;
import java.util.Map;

/**
 * @author liuwei
 * Created by liuwei on 2018/11/28.
 */
@Component
public class TemplateService {

    @Autowired
    private Configuration freeMarkerConfiguration;
    @Autowired
    private FreeMarkerProperties freeMarkerProperties;

    private ThreadLocal<StringWriter> stringWriterThreadLocal = ThreadLocal.withInitial(() -> new StringWriter(10240));

    public String merge(Map<String, Object> model, String view) {
        Template template;
        try {
            template = freeMarkerConfiguration.getTemplate(view + freeMarkerProperties.getSuffix());
            StringWriter out = stringWriterThreadLocal.get();
            out.getBuffer().setLength(0);
            template.process(model, out);
            return out.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
