package io.liuwei.autumn.util;

import com.google.common.collect.Maps;
import io.liuwei.autumn.constant.Constants;
import io.liuwei.autumn.model.Article;
import io.liuwei.autumn.model.TreeNode;
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

    public static TreeNode toArticleTree(List<Article> articleList) {
        if (articleList == null) {
            return null;
        }

        TreeNode root = new TreeNode("/", Constants.HOMEPAGE_TITLE);
        if (articleList.size() == 0) {
            return root;
        }

        Map<String, TreeNode> path2node = Maps.newHashMapWithExpectedSize(articleList.size());
        for (Article article : articleList) {
            // 创建目录节点. e.g. /a/b/c, 依次创建 /a/, /a/b/
            int slash1Pos = 0;
            int slash2Pos;
            TreeNode parent = root;
            String path = article.getPath();
            while ((slash2Pos = path.indexOf('/', slash1Pos + 1)) != -1) {
                String dirPath = path.substring(0, slash2Pos + 1);
                TreeNode dir = path2node.get(dirPath);
                if (dir == null) {
                    String dirName = path.substring(slash1Pos + 1, slash2Pos);
                    dir = new TreeNode(dirPath, dirName);
                    path2node.put(dirPath, dir);
                    parent.getChildren().add(dir);
                }
                parent = dir;
                slash1Pos = slash2Pos;
            }

            // 创建叶子节点
            TreeNode leaf = new TreeNode(path, article.getName());
            leaf.setTitle(article.getTitle());
            leaf.setCategory(article.getCategory());
            leaf.setTags(article.getTags());
            leaf.setCreated(article.getCreated());
            leaf.setModified(article.getModified());
            parent.getChildren().add(leaf);
        }

        sortAllChildren(root, Comparator
                // 目录排前面
                .comparingInt((TreeNode o) -> (o.getChildren().size() > 0 ? 0 : 1))
                // 然后按字母顺序排序
                .thenComparing(TreeNode::getPath));
        return root;
    }

    private static void sortAllChildren(TreeNode root, Comparator<TreeNode> comparator) {
        Stack<TreeNode> stack = new Stack<>();
        stack.push(root);
        while (!stack.empty()) {
            TreeNode node = stack.pop();
            node.getChildren().sort(comparator);
            node.getChildren().forEach(o -> {
                if (o.getChildren().size() > 0) {
                    stack.push(o);
                }
            });
        }
    }

    public static void buildTreeHtml(List<TreeNode> nodes, String contentPath, StringBuilder builder) {
        if (CollectionUtils.isEmpty(nodes)) {
            return;
        }

        builder.append("<ul>");

        for (TreeNode node : nodes) {
            // begin node
            builder.append("<li class=\"tree_node");
            if (!CollectionUtils.isEmpty(node.getChildren())) {
                builder.append(" tree_node_dir tree_node_unfolded");
            } else {
                builder.append(" tree_node_leaf");
            }
            builder.append("\">");

            // begin header
            builder.append("<div class=\"tree_node_header\">");

            // icon
            builder.append("<span class=\"tree_node_header_icon no_selection\"></span>");

            // title
            if (!CollectionUtils.isEmpty(node.getChildren())) {
                builder.append("<span class=\"tree_node_header_name no_selection\">")
                        .append(node.getName())
                        .append("</span>");
            } else {
                builder.append("<a href=\"").append(contentPath).append(node.getPath()).append("\">")
                        .append(node.getName())
                        .append("</a>");
            }

            // end header
            builder.append("</div>");

            buildTreeHtml(node.getChildren(), contentPath, builder);

            // end node
            builder.append("</li>");
        }
        builder.append("</ul>");
    }
}
