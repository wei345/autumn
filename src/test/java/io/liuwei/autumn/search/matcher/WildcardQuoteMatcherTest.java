package io.liuwei.autumn.search.matcher;

import io.liuwei.autumn.search.model.Hit;
import org.junit.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;


/**
 * @author liuwei
 * Created by liuwei on 2018/12/13.
 */
public class WildcardQuoteMatcherTest {

    @Test
    public void find() {
        // 测试 "j*" 匹配 java
        String s = "\"j*\"";
        String text = "java";
        WildcardQuoteMatcher.Parser parser = new WildcardQuoteMatcher.Parser();
        assertThat(parser.accept(s, 0)).isTrue();
        assertThat(parser.getToken() instanceof WildcardQuoteMatcher).isTrue();
        WildcardQuoteMatcher matcher = (WildcardQuoteMatcher) parser.getToken();
        List<Hit> hitList = WildcardQuoteMatcher.findHitList(text, matcher.getSearches());
        assertThat(hitList.size()).isEqualTo(1);
        assertThat(hitList.get(0).getStart()).isEqualTo(0);
        assertThat(hitList.get(0).getEnd()).isEqualTo(text.length());
    }
}