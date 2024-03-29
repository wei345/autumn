package io.liuwei.autumn.service;

import com.google.common.hash.Hashing;
import com.vip.vjtools.vjkit.number.RandomUtil;
import io.liuwei.autumn.Application;
import io.liuwei.autumn.config.AppProperties;
import io.liuwei.autumn.model.User;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.DigestUtils;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.UUID;
import java.util.function.Supplier;

import static com.vip.vjtools.vjkit.text.EncodeUtil.decodeHex;
import static com.vip.vjtools.vjkit.text.EncodeUtil.encodeHex;
import static io.liuwei.autumn.service.UserService.passwordDigest1;
import static io.liuwei.autumn.service.UserService.passwordDigest2;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author liuwei
 * Created by liuwei on 2018/11/22.
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = Application.class)
@SuppressWarnings({"UnusedReturnValue", "UnstableApiUsage"})
public class UserServiceTest {

    @Autowired
    private AppProperties.RememberMe rememberMe;

    @Test
    public void generateUser() {
        // generate
        String username = "Username";
        String salt = encodeHex(Hashing.md5().hashString(RandomUtil.randomStringFixLength(1024), UTF_8).asBytes());
        byte[] saltBytes = decodeHex(salt);
        String plainPassword = RandomUtil.randomStringFixLength(16);
        String password = encodeHex(passwordDigest2(passwordDigest1(plainPassword, saltBytes), saltBytes));

        // print
        String userString = 1 + " " + username + " " + password + " " + salt;
        System.out.println(userString);
        System.out.println("password: " + plainPassword);
        AppProperties.Access access = new AppProperties.Access();
        access.setUsers(Collections.singletonList(userString));

        // check password
        UserService userService = new UserService("autumn.", access, rememberMe);

        User user = userService.getUser(username);
        assertThat(user.getUsername()).isEqualTo(username);
        assertThat(userService.checkPlainPassword(user, plainPassword)).isTrue();
        assertThat(userService.checkPlainPassword(user, plainPassword + "1")).isFalse();
    }

    @Test
    public void testSpeed() {
        String raw = UUID.randomUUID().toString();

        // 都是 0 ms
        for (int i = 0; i < 10; i++) {
            timing("md5", () -> DigestUtils.md5DigestAsHex(raw.getBytes(StandardCharsets.UTF_8)));
            timing("sha1", () -> Hashing.sha1().hashString(raw, StandardCharsets.UTF_8).toString());
            timing("sha256", () -> Hashing.sha256().hashString(raw, StandardCharsets.UTF_8).toString());
            timing("sha512", () -> Hashing.sha512().hashString(raw, StandardCharsets.UTF_8).toString());
        }
    }

    private <T> T timing(String name, Supplier<T> supplier) {
        long start = System.currentTimeMillis();
        T v = supplier.get();
        long cost = System.currentTimeMillis() - start;
        System.out.println(v);
        System.out.println(name + " cost " + cost + " ms");
        System.out.println();
        return v;
    }

    /* 未用
    interface LoginToken {
        boolean acquire(String clientIp);

        default void success(String clientIp) {
        }

        default void fail(String clientIp) {
        }
    }

    class MemoryLoginToken implements LoginToken {
        private int maxFailures = 10;

        private int failuresRememberSeconds = 86400;

        private Cache<String, Integer> cache = CacheBuilder
                .newBuilder()
                .maximumSize(100_000_000)
                .expireAfterWrite(failuresRememberSeconds, TimeUnit.SECONDS)
                .build();

        @Override
        public synchronized boolean acquire(String clientIp) {
            Integer remaining = cache.getIfPresent(clientIp);
            if (remaining == null) {
                cache.put(clientIp, maxFailures - 1);
                return true;
            }
            if (remaining > 0) {
                cache.put(clientIp, remaining - 1);
                return true;
            } else {
                return false;
            }
        }

        @Override
        public synchronized void success(String clientIp) {
            Integer remaining = cache.getIfPresent(clientIp);
            if (remaining == null) {
                return;
            }
            remaining++;
            if (remaining >= maxFailures) {
                cache.invalidate(clientIp);
            } else {
                cache.put(clientIp, remaining);
            }
        }
    }*/
}