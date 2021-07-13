package io.liuwei.autumn.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;

@Data
public class TreeNode {

    /**
     * 以 "/" 结尾
     */
    private final String path;

    private String name;

    private String title;

    private String category;

    private Set<String> tags;

    private Date created;

    private Date modified;

    private List<TreeNode> children = new ArrayList<>();

    public TreeNode(String path, String name) {
        this.name = name;
        this.path = path;
    }

}