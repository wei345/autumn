package xyz.liuw.autumn.service;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import xyz.liuw.autumn.Application;
import xyz.liuw.autumn.search.SearchResult;
import xyz.liuw.autumn.search.SearchingPage;

import java.util.Set;

/**
 * @author liuwei
 * Created by liuwei on 2018/11/28.
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = {Application.class})
public class SearchServiceTest {

    @Autowired
    private SearchService searchService;

    @Test
    public void search() {

        SearchResult sr = searchService.search("java spring"); // 搜索 1100+ Page，找到 133 结果，90 ms
        System.out.println(sr.getPages().size() + "|" + sr.getPages());
        searchService.search("java spring"); // 有 Page 级缓存，1 ms
        searchService.search("java spring"); // 1 ms

    }
}