package xyz.liuw.autumn.data;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;

class TreeNode {
    String path;
    String name;
    private String title;
    private String category;
    private Set<String> tags;
    private Date created;
    private Date modified;
    List<TreeNode> children;

    @JsonIgnore
    TreeNode parent;
    @JsonIgnore
    private Page page;
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

    public void setPage(Page page) {
        this.page = page;
        this.title = page.getTitle();
        this.category = page.getCategory();
        this.tags = page.getTags();
        this.created = page.getCreated();
        this.modified = page.getModified();
    }

    boolean isDir() {
        return dir;
    }

    public Page getPage() {
        return page;
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

    public String getTitle() {
        return title;
    }

    public String getCategory() {
        return category;
    }

    public Set<String> getTags() {
        return tags;
    }

    public Date getCreated() {
        return created;
    }

    public Date getModified() {
        return modified;
    }
}