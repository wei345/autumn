package xyz.liuw.autumn;

/**
 * @author liuwei
 * Created by liuwei on 2018/11/12.
 */
public enum Group {

    USER(1), ALL(2);

    private int level;

    Group(int level) {
        this.level = level;
    }

    public static Group fromString(String s) {
        return Group.valueOf(s.toUpperCase());
    }

    public static boolean allow(Group require, Group subject) {
        if (require == null || subject == null) {
            return false;
        }
        return require.level >= subject.level;
    }

}
