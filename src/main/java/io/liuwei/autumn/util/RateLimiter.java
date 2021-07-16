package io.liuwei.autumn.util;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;

import java.util.Collections;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author liuwei
 * @since 2018-11-29
 */
@Slf4j
public class RateLimiter {

    /* KEYS[1]=key,ARGV[1]=limit,ARGV[2]=timeWindow,return=result */
    private static final String redisLimitScript =
            "local key = KEYS[1] " +
                    "local limit = tonumber(ARGV[1]) " +
                    "local timeWindow = ARGV[2] " +
                    "local current = tonumber(redis.call('incr', key)) " +
                    "if current > limit then " +
                    "   return 0 " +
                    "elseif current == 1 then " +
                    "   redis.call('expire', key, timeWindow) " +
                    "end " +
                    "return 1 ";
    private static final RedisScript<Boolean> redisScript = new DefaultRedisScript<>(redisLimitScript, Boolean.class);
    private final int limit;
    private final int timeWindowInSeconds;
    private final StringRedisTemplate stringRedisTemplate;
    private Cache<String, AtomicInteger> cache;

    public RateLimiter(int limit, int timeWindowInSeconds, int maxSize, StringRedisTemplate stringRedisTemplate) {
        this.limit = limit;
        this.timeWindowInSeconds = timeWindowInSeconds;
        this.stringRedisTemplate = stringRedisTemplate;
        if (stringRedisTemplate == null) {
            cache = CacheBuilder
                    .newBuilder()
                    .expireAfterWrite(timeWindowInSeconds, TimeUnit.SECONDS)
                    .maximumSize(maxSize)
                    .build();
        }
        log.info("Using {}", isRedisEnabled() ? "Redis" : "guava Cache");
    }

    public boolean acquire(String key) {
        if (isRedisEnabled()) {
            return redisAcquire(key);
        } else {
            return cacheAcquire(key);
        }
    }

    private boolean redisAcquire(String key) {
        return Boolean.TRUE.equals(stringRedisTemplate.execute(
                redisScript,
                Collections.singletonList(key),
                String.valueOf(limit),
                String.valueOf(timeWindowInSeconds)));
    }

    private boolean cacheAcquire(String key) {
        try {
            return cache
                    .get(key, () -> new AtomicInteger(0))
                    .addAndGet(1) <= limit;
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    private boolean isRedisEnabled() {
        return stringRedisTemplate != null;
    }
}
