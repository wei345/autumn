package io.liuwei.autumn.converter;

import io.liuwei.autumn.model.ArticleHtml;

/**
 * @author liuwei
 * Created by liuwei on 2018/11/30.
 */
public interface PageConverter {

    ArticleHtml convert(String title, String body);
}
