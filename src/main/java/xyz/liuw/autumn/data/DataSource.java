package xyz.liuw.autumn.data;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * @author liuwei
 * Created by liuwei on 2018/11/19.
 */
@Component
public class DataSource {
    volatile PageData pageData = PageData.EMPTY;
    volatile PageData pageDataPublished = PageData.EMPTY;

    public TreeJson getTreeJson() {
        return pageData.treeJson;
    }

    public Page getPage(String path) {
        return pageData.pageMap.get(path);
    }

    public TreeJson getTreeJsonPublished() {
        return pageDataPublished.treeJson;
    }

    public Page getPagePublished(String path) {
        return pageDataPublished.pageMap.get(path);
    }
}

class PageData {
    static final PageData EMPTY = new PageData(TreeJson.EMPTY, Collections.emptyMap());

    TreeJson treeJson;
    // path -> Page
    Map<String, Page> pageMap;

    PageData(TreeJson treeJson, Map<String, Page> pageMap) {
        this.treeJson = treeJson;
        this.pageMap = pageMap;
    }
}

class TreeNode {
    String name;
    String path;
    List<TreeNode> children;
    @JsonIgnore
    TreeNode parent;
    @JsonIgnore
    private boolean dir;
    @JsonIgnore
    Page page;

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