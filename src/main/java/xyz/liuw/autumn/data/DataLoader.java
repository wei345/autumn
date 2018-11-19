package xyz.liuw.autumn.data;

import com.google.common.collect.Maps;
import com.vip.vjtools.vjkit.mapper.JsonMapper;
import com.vip.vjtools.vjkit.number.MathUtil;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import javax.annotation.PostConstruct;
import java.io.File;
import java.util.*;

/**
 * @author liuwei
 * Created by liuwei on 2018/11/19.
 */
@Component
public class DataLoader {

    private static Logger logger = LoggerFactory.getLogger(DataLoader.class);

    @Autowired
    private DataSource dataSource;
    @Autowired
    private JsonMapper jsonMapper;
    @Value("${autumn.data-dir}")
    private String dataDir;
    @Value("${autumn.data.reload-interval-seconds:10}")
    private long reloadIntervalSeconds;
    private int reloadContinuousFailures; // default 0

    @PostConstruct
    void start() {
        load();
        timingReload();
    }

    private void timingReload() {
        new Thread(() -> {
            logger.info("Started loadPageData thread");
            while (reloadIntervalSeconds > 0) {
                try {
                    long t = reloadIntervalSeconds * 1000 * MathUtil.pow(2, reloadContinuousFailures);
                    Thread.sleep(t);
                } catch (InterruptedException ignore) {
                }
                try {
                    load();
                    reloadContinuousFailures = 0;
                } catch (Exception e) {
                    if (reloadContinuousFailures < 10) {
                        reloadContinuousFailures++;
                    }
                }
            }
        }, "loadPageData").start();
    }

    void load() {
        Validate.notBlank(dataDir, "config 'autumn.data-dir' is empty");
        logger.info("Loading {}", dataDir);
        long start = System.currentTimeMillis();
        load(new File(dataDir));
        logger.info("Loaded in {} ms", System.currentTimeMillis() - start);
    }

    void load(File rootDir) {
        Validate.notNull(rootDir, "rootDir is null");
        Validate.isTrue(!rootDir.isHidden(), "rootDir is hidden");
        Validate.isTrue(rootDir.isDirectory(), "rootDir is not a directory");

        Map<String, Page> oldPageMap = dataSource.pageData.pageMap;
        int size = ((oldPageMap == null || oldPageMap.size() == 0) ? 1500 : oldPageMap.size());
        Map<String, Page> pageMap = Maps.newHashMapWithExpectedSize(size);

        TreeNode root = new TreeNode("", "/", true);
        Stack<File> dirStack = new Stack<>();
        Stack<TreeNode> nodeStack = new Stack<>();
        dirStack.push(rootDir);
        nodeStack.push(root);
        while (!dirStack.isEmpty()) {
            File dir = dirStack.pop();
            TreeNode parent = nodeStack.pop();

            //noinspection ConstantConditions
            for (File file : dir.listFiles()) {
                if (file.isHidden() || file.getAbsolutePath().startsWith(".")) {
                    continue;
                }

                if (file.isDirectory()) {
                    String path = parent.path + file.getName() + "/";
                    TreeNode node = new TreeNode(file.getName(), path, true);
                    parent.addChild(node);
                    dirStack.push(file);
                    nodeStack.push(node);
                    continue;
                }

                if (file.isFile() && file.getName().endsWith(".md")) {
                    String name = filename(file);
                    String path = parent.path + name;

                    // parse page
                    if ("/index".equals(path) || "/sidebar".equals(path)) {
                        // skip dokuwiki page
                        continue;
                    }
                    Page page;
                    if (oldPageMap != null &&
                            (page = oldPageMap.get(path)) != null &&
                            page.getLastModified() == file.lastModified()) {
                    } else {
                        page = PageParser.parse(file);
                    }
                    pageMap.put(path, page);

                    // add node
                    TreeNode node = new TreeNode(name, path, false);
                    node.page = page;
                    parent.addChild(node);
                }
            }
        }

        sortAndRemoveEmptyNode(root);

        String json = jsonMapper.toJson(root);
        dataSource.pageData = new PageData(new TreeJson(json), pageMap);
        setPageDataPublished(root);
    }

    private String filename(File f) {
        String s = f.getName();
        int n = s.lastIndexOf(".");
        if (n == -1) {
            return s;
        }
        return s.substring(0, n);
    }

    private void sortAndRemoveEmptyNode(TreeNode root) {
        Stack<TreeNode> allDirNodes = new Stack<>();

        Stack<TreeNode> dirStack = new Stack<>();
        dirStack.push(root);
        while (!dirStack.empty()) {
            TreeNode node = dirStack.pop();
            if (CollectionUtils.isEmpty(node.children)) {
                continue;
            }

            // 目录排在前面，然后按字母顺序
            node.children.sort((o1, o2) -> {
                if (o1.isDir() && !o2.isDir()) {
                    return -1;
                }
                if (!o1.isDir() && o2.isDir()) {
                    return 1;
                }
                return o1.name.compareTo(o2.name);
            });

            for (TreeNode nd : node.children) {
                if (nd.isDir()) {
                    allDirNodes.push(nd);
                    dirStack.push(nd);
                }
            }
        }

        removeEmptyDirNode(allDirNodes);
    }

    private void removeEmptyDirNode(Stack<TreeNode> dirNodes) {
        while (!dirNodes.empty()) {
            TreeNode dirNode = dirNodes.pop();
            if (CollectionUtils.isEmpty(dirNode.children)) {
                dirNode.parent.children.remove(dirNode);
            }
        }
    }

    private void setPageDataPublished(TreeNode root) {
        Map<String, Page> pageMap = new HashMap<>();
        Stack<TreeNode> allDirNodes = new Stack<>();

        // remove non-published page
        Stack<TreeNode> dirStack = new Stack<>();
        dirStack.push(root);
        while (!dirStack.empty()) {
            TreeNode node = dirStack.pop();
            if (CollectionUtils.isEmpty(node.children)) {
                continue;
            }

            List<TreeNode> publishedList = new ArrayList<>(node.children.size());
            for (TreeNode nd : node.children) {
                if (nd.isDir()) {
                    publishedList.add(nd);
                    dirStack.push(nd);
                    allDirNodes.push(nd);
                } else if (nd.page.isPublished()) {
                    pageMap.put(nd.getPath(), nd.page);
                    publishedList.add(nd);
                }
            }
            node.children = publishedList;
        }

        removeEmptyDirNode(allDirNodes);

        String json = jsonMapper.toJson(root);
        dataSource.pageDataPublished = new PageData(new TreeJson(json), pageMap);
    }
}
