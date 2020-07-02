package io.liuwei.autumn.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;

@Getter
public class TreeNode {
    @JsonIgnore
    private final boolean dir;

    private final String path;

    private final String name;

    @Setter
    private List<TreeNode> children;

    @JsonIgnore
    private TreeNode parent;

    private String title;

    private String category;

    private Set<String> tags;

    private Date created;

    private Date modified;

    @Setter
    private boolean generated;

    @JsonIgnore
    private Page page;

    public TreeNode(String name, String path, boolean dir) {
        this.name = name;
        this.path = path;
        this.dir = dir;
    }

    public void addChild(TreeNode child) {
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
        this.generated = page.isGenerated();
    }
}