package io.liuwei.autumn.search;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.*;

/**
 * @author liuwei602099
 * @since 2021-07-16 19:08
 */
public class SearcherTest {

    @Test
    public void sort() {

        List<Foo> list = new ArrayList<>();
        list.add(new Foo(1, 0, 1, 3));
        list.add(new Foo(2, 3, 1, 1));
        list.add(new Foo(3, 2, 2, 2));
        list.add(new Foo(4, 2, 4, 2));
        list.add(new Foo(5, 0, 1, 1));

        // order by nameHitCount desc, titleHitCount desc, pathHitCount desc
        list.sort(Comparator
                .comparing(Foo::getNameHitCount).reversed()
                .thenComparing(Comparator.comparing(Foo::getTitleHitCount).reversed())
                .thenComparing(Comparator.comparing(Foo::getPathHitCount).reversed())
        );
        assertThat(list.stream().map(Foo::getId).collect(Collectors.toList()))
                .isEqualTo(Arrays.asList(2, 4, 3, 1, 5));
    }

    @Data
    @AllArgsConstructor
    private static class Foo {
        private int id;
        private int nameHitCount;
        private int titleHitCount;
        private int pathHitCount;
    }

}