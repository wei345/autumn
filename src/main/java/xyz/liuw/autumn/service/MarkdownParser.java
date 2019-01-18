package xyz.liuw.autumn.service;

import xyz.liuw.autumn.data.Page;

/**
 * @author liuwei
 * Created by liuwei on 2018/11/30.
 */
public interface MarkdownParser {

    Page.PageHtml render(String title, String body, String path);
}
