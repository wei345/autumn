package io.liuwei.autumn.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;

@Data
public class ArticleTreeNode {

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

    private List<ArticleTreeNode> children = new ArrayList<>();

    public ArticleTreeNode(String path, String name) {
        this.name = name;
        this.path = path;
    }

}