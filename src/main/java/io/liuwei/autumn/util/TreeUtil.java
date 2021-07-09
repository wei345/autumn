package io.liuwei.autumn.util;

import com.google.common.collect.Maps;
import io.liuwei.autumn.model.Article;
import io.liuwei.autumn.model.ArticleTreeNode;
import org.springframework.util.CollectionUtils;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Stack;

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
        sort(root, Comparator
                // 目录排前面
                .comparingInt((ArticleTreeNode o) -> (o.getChildren().size() > 0 ? 0 : 1))
                // 然后按字母顺序排序
                .thenComparing(ArticleTreeNode::getPath));
        return root;
    }

    private static void sort(ArticleTreeNode root, Comparator<ArticleTreeNode> comparator) {
        Stack<ArticleTreeNode> stack = new Stack<>();
        stack.push(root);
        while (!stack.empty()) {
            ArticleTreeNode node = stack.pop();
            node.getChildren().sort(comparator);
            node.getChildren().forEach(o -> {
                if (o.getChildren().size() > 0) {
                    stack.push(o);
                }
            });
        }
    }

    public static void buildTreeHtml(List<ArticleTreeNode> nodes, String contentPath, StringBuilder stringBuilder) {
        if (CollectionUtils.isEmpty(nodes)) {
            return;
        }

        stringBuilder.append("<ul>");

        for (ArticleTreeNode node : nodes) {
            // begin node
            stringBuilder.append("<li class=\"tree_node");
            if (!CollectionUtils.isEmpty(node.getChildren())) {
                stringBuilder.append(" tree_node_dir tree_node_unfolded");
            } else {
                stringBuilder.append(" tree_node_leaf");
            }
            stringBuilder.append("\">");

            // begin header
            stringBuilder.append("<div class=\"tree_node_header\">");

            // icon
            stringBuilder.append("<span class=\"tree_node_header_icon no_selection\"></span>");

            // title
            if (!CollectionUtils.isEmpty(node.getChildren())) {
                stringBuilder.append("<span class=\"tree_node_header_name no_selection\">")
                        .append(node.getName())
                        .append("</span>");
            } else {
                stringBuilder.append("<a href=\"").append(contentPath).append(node.getPath()).append("\">")
                        .append(node.getName())
                        .append("</a>");
            }

            // end header
            stringBuilder.append("</div>");

            buildTreeHtml(node.getChildren(), contentPath, stringBuilder);

            // end node
            stringBuilder.append("</li>");
        }
        stringBuilder.append("</ul>");
    }
}
