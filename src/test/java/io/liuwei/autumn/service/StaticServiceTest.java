package io.liuwei.autumn.service;

import org.junit.Test;

import java.io.File;
import java.net.URL;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author liuwei
 * Created by liuwei on 2018/11/19.
 */
public class StaticServiceTest {

    @Test
    public void getTemplateLastModified() {
        String templateLoaderPath = "/templates/";
        String name = "article.html";

        URL url = getClass().getResource(templateLoaderPath + name);
        assertThat(url).isNotNull();

        File file = new File(url.getPath());
        assertThat(file.exists()).isTrue();

        System.out.println(file.lastModified());
    }
}