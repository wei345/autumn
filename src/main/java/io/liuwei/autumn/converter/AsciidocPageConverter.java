package io.liuwei.autumn.converter;

import io.liuwei.autumn.domain.Page;
import io.liuwei.autumn.service.DataService;
import io.liuwei.autumn.util.Asciidoctors;
import org.asciidoctor.AttributesBuilder;
import org.asciidoctor.OptionsBuilder;
import org.springframework.stereotype.Component;

/**
 * @author liuwei
 * @since 2020-06-01 18:45
 */
@Component
public class AsciidocPageConverter extends AbstractPageConverter {

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
        String bodyHtml = Asciidoctors.getAsciidoctor().convert(body, optionsBuilder);

        return new Page.PageHtml(null, null, bodyHtml);
    }
}
