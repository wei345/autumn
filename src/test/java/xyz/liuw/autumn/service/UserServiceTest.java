package xyz.liuw.autumn.service;

import com.vip.vjtools.vjkit.number.RandomUtil;
import org.junit.Test;
import org.springframework.util.DigestUtils;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

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
        String salt = UUID.randomUUID().toString();
        String plainPassword = RandomUtil.randomStringFixLength(14);
        String s = plainPassword + salt;
        String password = DigestUtils.md5DigestAsHex(s.getBytes(StandardCharsets.UTF_8));

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
}