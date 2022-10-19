package io.liuwei.autumn.component;

import io.liuwei.autumn.model.Article;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.cache.interceptor.SimpleKeyGenerator;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.stream.Stream;

/**
 * @author liuwei602099
 * @since 2021-07-12 17:18
 */
@SuppressWarnings("ALL")
@Component
public class CacheKeyGenerator implements KeyGenerator {
    @Override
    public Object generate(Object target, Method method, Object... params) {
        Object obj =  SimpleKeyGenerator
                .generateKey(Stream
                        .of(params)
                        .map(param -> {
                            if (param instanceof Article) {
                                return ((Article) param).getSnapshotId();
                            } else {
                                return param;
                            }
                        })
                        .toArray());
        return obj;
    }

}
