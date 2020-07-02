package io.liuwei.autumn.converter;

import io.liuwei.autumn.domain.Page;
import io.liuwei.autumn.service.DataService;
import org.asciidoctor.Asciidoctor;
import org.asciidoctor.AttributesBuilder;
import org.asciidoctor.OptionsBuilder;
import org.asciidoctor.jruby.internal.JRubyAsciidoctor;
import org.springframework.stereotype.Component;

/**
 * @author liuwei
 * @since 2020-06-01 18:45
 */
@Component
public class AsciidocPageConverter extends AbstractPageConverter {

    private final Asciidoctor asciidoctor = new JRubyAsciidoctor();

    private final OptionsBuilder optionsBuilder = OptionsBuilder.options()
            .attributes(AttributesBuilder.attributes()
                    .showTitle(true)
                    .tableOfContents(true));

    public AsciidocPageConverter(DataService dataService) {
        super(dataService);
    }

    @Override
    protected Page.PageHtml parse(String title, String body) {
        body = "= " + title + "\n\n" + body;
        String bodyHtml = asciidoctor.convert(body, optionsBuilder);

        return new Page.PageHtml(null, null, bodyHtml);
    }
}
