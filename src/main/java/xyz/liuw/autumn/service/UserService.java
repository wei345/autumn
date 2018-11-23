package xyz.liuw.autumn.service;

import com.google.common.collect.Maps;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.DigestUtils;
import xyz.liuw.autumn.domain.User;

import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * @author liuwei
 * Created by liuwei on 2018/11/22.
 */
@Component
public class UserService {

    private Map<String, User> users;
    private Map<Long, User> idUser;

    @Value("${autumn.users}")
    void setUsers(String input) {
        Validate.notBlank(input, "config 'autumn.users' is blank");
        users = Maps.newHashMapWithExpectedSize(2);
        idUser = Maps.newHashMapWithExpectedSize(2);
        for (String s : input.trim().split("\\s*;\\s*")) {
            if (StringUtils.isBlank(s)) {
                continue;
            }
            String[] parts = s.split("\\s+");
            long id = Long.valueOf(parts[0]);
            String username = parts[1];
            String password = parts[2];
            String salt = parts[3];
            User user = new User(id, username, password, salt);
            Validate.isTrue(users.get(username) == null, "Duplicate username '%s'", username);
            Validate.isTrue(idUser.get(id) == null, "Duplicate id '%s'", id);
            users.put(username, user);
            idUser.put(id, user);
        }
    }

    public User checkPassword(Long id, String password) {
        User user = idUser.get(id);
        return checkPassword(user, password);
    }

    private User checkPassword(User user, String password) {
        if (user == null) {
            return null;
        }
        String s = password + user.getSalt();
        String md5 = DigestUtils.md5DigestAsHex(s.getBytes(StandardCharsets.UTF_8));
        if (md5.equals(user.getPassword())) {
            return user;
        } else {
            return null;
        }
    }

    public User checkPassword(String username, String password) {
        User user = users.get(username);
        return checkPassword(user, password);
    }

    public User getUser(Long id) {
        return idUser.get(id);
    }

    public User getUser(String username) {
        return users.get(username);
    }

}
