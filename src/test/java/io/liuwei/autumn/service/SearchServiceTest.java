package io.liuwei.autumn.service;

import io.liuwei.autumn.Application;
import io.liuwei.autumn.enums.AccessLevelEnum;
import io.liuwei.autumn.search.model.SearchResult;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

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

        SearchResult sr = searchService.search("java spring", AccessLevelEnum.OWNER, 0, Integer.MAX_VALUE); // 搜索 1100+ Page，找到 133 结果，90 ms
        System.out.println(sr.getPages().size() + "|" + sr.getPages());
        searchService.search("java spring", AccessLevelEnum.OWNER, 0, Integer.MAX_VALUE); // 有 Page 级缓存，1 ms
        searchService.search("java spring", AccessLevelEnum.OWNER, 0, Integer.MAX_VALUE); // 1 ms

    }
}