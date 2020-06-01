package io.liuwei.autumn.service;

import io.liuwei.autumn.data.Page;

/**
 * @author liuwei
 * Created by liuwei on 2018/11/30.
 */
public interface PageConverter {

    Page.PageHtml convert(String title, String body, String path);
}
