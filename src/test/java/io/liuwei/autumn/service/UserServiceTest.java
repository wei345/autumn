package io.liuwei.autumn.service;

import com.google.common.hash.Hashing;
import com.vip.vjtools.vjkit.number.RandomUtil;
import org.junit.Test;
import org.springframework.util.DigestUtils;

import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.function.Supplier;

import static com.vip.vjtools.vjkit.text.EncodeUtil.decodeHex;
import static com.vip.vjtools.vjkit.text.EncodeUtil.encodeHex;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static io.liuwei.autumn.service.UserService.passwordDigest1;
import static io.liuwei.autumn.service.UserService.passwordDigest2;

/**
 * @author liuwei
 * Created by liuwei on 2018/11/22.
 */
@SuppressWarnings({"UnusedReturnValue", "UnstableApiUsage"})
public class UserServiceTest {

    @Test
    public void generateUser() {
        // generate
        String username = "Username";
        String salt = encodeHex(Hashing.md5().hashString(RandomUtil.randomStringFixLength(1024), UTF_8).asBytes());
        byte[] saltBytes = decodeHex(salt);
        String plainPassword = RandomUtil.randomStringFixLength(16);
        String password = encodeHex(passwordDigest2(passwordDigest1(plainPassword, saltBytes), saltBytes));

        // print
        String userString = 1 + " " + username + " " + password + " " + salt + ";";
        System.out.println(userString);
        System.out.println("password: " + plainPassword);

        // check password
        UserService userService = new UserService();
        userService.setUsers(userString);
        assertThat(userService.getUser(username)).isNotNull();
        UserService.User user = userService.checkPlainPassword(username, plainPassword);
        assertThat(user.getUsername()).isEqualTo(username);
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

    @Test
    public void testKey() {
        String salt = RandomUtil.randomStringFixLength(16);
        String plainPassword = RandomUtil.randomStringFixLength(16);
        byte[] key = salt.getBytes(UTF_8);
        assertThat(Hashing.hmacSha1(key).hashString(plainPassword, UTF_8).toString()).isNotEqualTo(Hashing.sha1().hashString(plainPassword, UTF_8).toString());
        assertThat(Hashing.hmacSha1(key).hashString(plainPassword, UTF_8).toString()).isNotEqualTo(Hashing.sha1().hashString(plainPassword + salt, UTF_8).toString());
        assertThat(Hashing.hmacSha256(key).hashString(plainPassword, UTF_8).toString()).isNotEqualTo(Hashing.sha256().hashString(plainPassword, UTF_8).toString());
        assertThat(Hashing.hmacSha256(key).hashString(plainPassword, UTF_8).toString()).isNotEqualTo(Hashing.sha256().hashString(plainPassword + salt, UTF_8).toString());
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