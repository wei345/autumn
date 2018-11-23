package xyz.liuw.autumn.domain;

/**
 * @author liuwei
 * Created by liuwei on 2018/11/22.
 */
public class User {
    private Long id;
    private String username;
    private String password;
    private String salt;

    // used by Jackson deserialize
    public User() {
    }

    public User(long id, String username, String password, String salt) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.salt = salt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getSalt() {
        return salt;
    }

    public void setSalt(String salt) {
        this.salt = salt;
    }
}
