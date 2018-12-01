package xyz.liuw.autumn.service;

/**
 * @author liuwei
 * Created by liuwei on 2018/11/30.
 */
public interface MarkdownParser {
    String render(String path, String title, String body);
    String render(String title, String body);
    String render(String source);
}
