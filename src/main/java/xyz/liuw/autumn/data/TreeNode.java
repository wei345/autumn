package xyz.liuw.autumn.data;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.ArrayList;
import java.util.List;

class TreeNode {
    String name;
    String path;
    List<TreeNode> children;
    @JsonIgnore
    TreeNode parent;
    @JsonIgnore
    Page page;
    @JsonIgnore
    private boolean dir;

    TreeNode(String name, String path, boolean dir) {
        this.name = name;
        this.path = path;
        this.dir = dir;
    }

    void addChild(TreeNode child) {
        if (children == null) {
            children = new ArrayList<>();
        }
        child.parent = this;
        children.add(child);
    }

    boolean isDir() {
        return dir;
    }

    // getters 用于 Jackson JSON 序列化
    public String getName() {
        return name;
    }

    public String getPath() {
        return path;
    }

    public List<TreeNode> getChildren() {
        return children;
    }

}