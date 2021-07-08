package io.liuwei.autumn.model;

import lombok.Data;

/**
 * @author liuwei
 * @since 2021-07-08 17:36
 */
@Data
public class User {
    private Long id;
    private String username;
    private String password;
    private String salt;
    private byte[] passwordBytes;
    private byte[] saltBytes;
    private Boolean isOwner;

    // used by Jackson deserialize
    public User() {
    }

    public User(long id, String username, String password, String salt, byte[] passwordBytes, byte[] saltBytes) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.salt = salt;
        this.passwordBytes = passwordBytes;
        this.saltBytes = saltBytes;
    }

}
