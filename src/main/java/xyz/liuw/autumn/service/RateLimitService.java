package xyz.liuw.autumn.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

import java.util.Collections;

/**
 * @author liuwei
 * Created by liuwei on 2018/11/29.
 */
@SuppressWarnings("ALL")
@Component
public class RateLimitService {

    private static final String PREFIX = "autumn:rate-limit";

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    private String searchLimit = "100";

    private String searchTimeWindowSeconds = "600";

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

    public boolean searchAcquire(String ip) {
        String key = PREFIX + ":search:" + ip;
        return acquire(key);
    }

    private boolean acquire(String key) {
        return Boolean.TRUE == stringRedisTemplate.execute(
                redisScript,
                Collections.singletonList(key),
                searchLimit,
                searchTimeWindowSeconds);
    }
}
