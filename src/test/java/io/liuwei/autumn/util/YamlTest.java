package io.liuwei.autumn.util;

import org.apache.commons.lang3.StringUtils;
import org.junit.Test;
import org.yaml.snakeyaml.Yaml;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * @author liuwei
 * @since 2026-04-04 20:16
 */
public class YamlTest {

    @Test
    public void test(){
        String yaml = "title: \"Quiz\"\n" +
                "author: \"John\"\n" +
                "output: pdf_document\n" +
                "date: \"2026-02-23\"";


        Object list = new Yaml().load(new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8)));

        System.out.println(list);
    }

    @Test
    public void test2(){
        List<String> lines = new ArrayList<>();
        lines.add("a");
        lines.add("b");
        System.out.println(StringUtils.join(lines, "\n"));
    }
}
