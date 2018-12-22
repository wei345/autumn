package xyz.liuw.autumn.service;

import com.google.common.hash.Hashing;
import com.vip.vjtools.vjkit.number.RandomUtil;
import org.junit.Test;
import org.springframework.util.DigestUtils;

import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;


/**
 * @author liuwei
 * Created by liuwei on 2018/11/22.
 */
public class UserServiceTest {

    @Test
    public void generateUser() {
        // generate
        String username = "Username";
        String salt = RandomUtil.randomStringFixLength(16);
        String plainPassword = RandomUtil.randomStringFixLength(16);
        String s = plainPassword + salt;
        @SuppressWarnings("UnstableApiUsage") String password = Hashing.sha512().hashString(s, StandardCharsets.UTF_8).toString();

        // print
        String userString = 1 + " " + username + " " + password + " " + salt + ";";
        System.out.println(userString);
        System.out.println("password: " + plainPassword);

        // check password
        UserService userService = new UserService();
        userService.setUsers(userString);
        assertThat(userService.getUser(username)).isNotNull();
        UserService.User user = userService.checkPassword(username, plainPassword);
        assertThat(user.getUsername()).isEqualTo(username);
    }

    @Test
    public void test() {
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
}