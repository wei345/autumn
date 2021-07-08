package io.liuwei.autumn.util;

import com.google.common.collect.Maps;
import io.liuwei.autumn.model.Article;
import io.liuwei.autumn.model.ArticleTreeNode;

import java.util.List;
import java.util.Map;

/**
 * @author liuwei
 * @since 2021-07-07 22:54
 */
public class TreeUtil {

    public static ArticleTreeNode toArticleTree(List<Article> articleList) {
        if (articleList == null) {
            return null;
        }
        ArticleTreeNode root = new ArticleTreeNode("/", "Home");
        if (articleList.size() == 0) {
            return root;
        }
        Map<String, ArticleTreeNode> path2node = Maps.newHashMapWithExpectedSize(articleList.size());

        for (Article article : articleList) {
            int slash1Pos = 0;
            int slash2Pos;
            ArticleTreeNode parent = root;
            String path = article.getPath();
            while ((slash2Pos = path.indexOf('/', slash1Pos + 1)) != -1) {
                String dirPath = path.substring(0, slash2Pos + 1);
                ArticleTreeNode dir = path2node.get(dirPath);
                if (dir == null) {
                    String dirName = path.substring(slash1Pos + 1, slash2Pos);
                    dir = new ArticleTreeNode(dirPath, dirName);
                    path2node.put(dirPath, dir);
                    parent.getChildren().add(dir);
                }
                parent = dir;
                slash1Pos = slash2Pos;
            }
            ArticleTreeNode leaf = new ArticleTreeNode(path, article.getName());
            leaf.setTitle(article.getTitle());
            leaf.setCategory(article.getCategory());
            leaf.setTags(article.getTags());
            leaf.setCreated(article.getCreated());
            leaf.setModified(article.getModified());
            parent.getChildren().add(leaf);
        }
        return root;
    }
}
