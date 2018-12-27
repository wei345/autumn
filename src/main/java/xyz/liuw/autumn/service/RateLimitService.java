package xyz.liuw.autumn.service;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author liuwei
 * Created by liuwei on 2018/11/29.
 */
@Component
public class RateLimitService {

    private static final String PREFIX = "autumn:rate-limit";

    private static Logger logger = LoggerFactory.getLogger(RateLimitService.class);

    @SuppressWarnings("SpringJavaAutowiredFieldsWarningInspection")
    @Autowired(required = false)
    private StringRedisTemplate stringRedisTemplate;

    @SuppressWarnings("FieldCanBeLocal")
    private int searchLimit = 100;

    @SuppressWarnings("FieldCanBeLocal")
    private int searchTimeWindowSeconds = 600;

    @SuppressWarnings("FieldCanBeLocal")
    private int loginLimit = 24;

    @SuppressWarnings("FieldCanBeLocal")
    private int loginTimeWindowSeconds = 86400;

    /* KEYS[1]=key,ARGV[1]=limit,ARGV[2]=timeWindow,return=result */
    private String redisLimitScript =
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

    private RedisScript<Boolean> redisScript = new DefaultRedisScript<>(redisLimitScript, Boolean.class);

    private ConcurrentHashMap<Integer, Cache<String, AtomicInteger>> expireToCache = new ConcurrentHashMap<>(4);

    @PostConstruct
    private void init() {
        if (isRedisEnabled()) {
            logger.info("Using Redis");
        } else {
            logger.info("Using guava Cache");
        }
    }

    public boolean acquireSearch(String ip) {
        String key = PREFIX + ":search:" + ip;
        return acquire(key, searchLimit, searchTimeWindowSeconds);
    }

    public boolean acquireLogin(String ip) {
        String key = PREFIX + ":login:" + ip;
        return acquire(key, loginLimit, loginTimeWindowSeconds);
    }

    private boolean acquire(String key, int limit, int timeWindowSeconds) {
        if (isRedisEnabled()) {
            return redisAcquire(key, limit, timeWindowSeconds);
        } else {
            return cacheAcquire(key, limit, timeWindowSeconds);
        }
    }

    private boolean isRedisEnabled() {
        return stringRedisTemplate != null;
    }

    private boolean cacheAcquire(String key, int limit, int timeWindowSeconds) {
        AtomicInteger counter;
        try {
            counter = getCache(timeWindowSeconds).get(key, () -> new AtomicInteger(0));
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        }
        return counter.addAndGet(1) <= limit;
    }

    private Cache<String, AtomicInteger> getCache(Integer expire) {
        return expireToCache.computeIfAbsent(expire, integer -> CacheBuilder
                .newBuilder()
                .expireAfterWrite(integer, TimeUnit.SECONDS)
                .build());
    }

    private boolean redisAcquire(String key, int limit, int timeWindowSeconds) {
        return Boolean.TRUE == stringRedisTemplate.execute(
                redisScript,
                Collections.singletonList(key),
                String.valueOf(limit),
                String.valueOf(timeWindowSeconds));
    }
}
