package io.liuwei.autumn.converter;

import io.liuwei.autumn.domain.Page;

/**
 * @author liuwei
 * Created by liuwei on 2018/11/30.
 */
public interface PageConverter {

    Page.PageHtml convert(String title, String body, String path);
}
