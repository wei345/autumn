package io.liuwei.autumn.converter;

import io.liuwei.autumn.domain.Page;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author liuwei
 * @since 2020-07-02 15:46
 */
public class AbstractPageConverterTest {

    @Test
    public void makeNumberedToc(){
        String toHtml = "<div class=\"toc\">\n" +
                "<h3 class=\"no_selection action_toggle\">TOC</h3>\n" +
                "<ul>\n" +
                "<li><a href=\"#hbase\">HBase</a>\n" +
                "<ul>\n" +
                "<li><a href=\"#data-model\">data model</a></li>\n" +
                "<li><a href=\"#how-it-works\">how it works</a>\n" +
                "<ul>\n" +
                "<li><a href=\"#regions\">regions</a></li>\n" +
                "<li><a href=\"#meta-table\">META table</a></li>\n" +
                "<li><a href=\"#client-put-or-get-row\">client put or get row</a></li>\n" +
                "</ul>\n" +
                "</li>\n" +
                "<li><a href=\"#写优化\">写优化</a></li>\n" +
                "<li><a href=\"#ddl\">DDL</a></li>\n" +
                "<li><a href=\"#资料\">资料</a></li>\n" +
                "</ul>\n" +
                "</li>\n" +
                "</ul>\n" +
                "</div>";
        AbstractPageConverter pageConverter = new AbstractPageConverter(null){
            @Override
            protected Page.PageHtml parse(String title, String body) {
                return null;
            }
        };
        String tocHtml2 = pageConverter.makeNumberedToc(toHtml);
        System.out.println(tocHtml2);
    }
}