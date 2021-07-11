package io.liuwei.autumn.converter;

import io.liuwei.autumn.model.ContentHtml;

/**
 * 把文章原文转为 HTML。
 *
 * @author liuwei
 * Created by liuwei on 2018/11/30.
 */
public interface ContentHtmlConverter {

    ContentHtml convert(String title, String content);
}
