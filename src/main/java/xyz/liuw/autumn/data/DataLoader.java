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
    private Page homepage;

    @PostConstruct
    void start() {
        homepage = newHomepage();
        load();
        timingReload();
    }

    private void timingReload() {
        String threadName = getClass().getSimpleName() + ".timingReload";
        new Thread(() -> {
            logger.info("Started '{}' thread", threadName);
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
        }, threadName).start();
    }

    @SuppressWarnings("WeakerAccess")
    void load() {
        Validate.notBlank(dataDir, "config 'autumn.data-dir' is empty");
        logger.info("Loading {}", dataDir);
        long start = System.currentTimeMillis();

        load(new File(dataDir));

        logger.info("Loaded in {} ms", System.currentTimeMillis() - start);
    }

    private void load(File rootDir) {
        Validate.notNull(rootDir, "rootDir is null");
        Validate.isTrue(!rootDir.isHidden(), "rootDir is hidden");
        Validate.isTrue(rootDir.isDirectory(), "rootDir is not a directory");

        Map<String, Page> oldPageMap = dataSource.getAllData().getPageMap();
        Map<String, Page> pageMap = Maps.newHashMapWithExpectedSize(
                oldPageMap.size() == 0 ? 1500 : oldPageMap.size());
        boolean pageAddedOrModified = false;

        Map<String, Media> oldMediaMap = dataSource.getAllData().getMediaMap();
        Map<String, Media> mediaMap = Maps.newHashMapWithExpectedSize(
                oldMediaMap.size() == 0 ? 100 : oldMediaMap.size());
        boolean mediaAddedOrModified = false;

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

                if (file.isFile()) {
                    String filename = file.getName();
                    if (filename.endsWith(".md")) {
                        String name = filename(file);
                        String path = parent.path + name;

                        // parse page
                        if ("/index".equals(path) || "/sidebar".equals(path)) {
                            // skip dokuwiki page
                            continue;
                        }
                        Page page = oldPageMap.get(path);
                        if (page == null || page.getLastModified() != file.lastModified()) {
                            pageAddedOrModified = true;
                            page = PageParser.parse(file);
                            page.setPath(path);
                        }
                        pageMap.put(path, page);

                        // add node
                        TreeNode node = new TreeNode(name, path, false);
                        node.page = page;
                        parent.addChild(node);
                    }

                    if (filename.endsWith(".png") || filename.endsWith(".jpg") || filename.endsWith(".ico")) {
                        String path = parent.path + file.getName();
                        Media media = oldMediaMap.get(path);
                        if (media == null || media.getLastModified() != file.lastModified()) {
                            mediaAddedOrModified = true;
                            media = new Media(file);
                        }
                        mediaMap.put(path, media);
                    }
                }
            }
        }
        pageMap.put("/", homepage);

        boolean pageChanged = pageAddedOrModified || pageMap.size() != oldPageMap.size();
        boolean mediaChanged = mediaAddedOrModified || mediaMap.size() != oldMediaMap.size();
        if (!pageChanged && !mediaChanged) {
            logger.info("dataSource no change");
            return;
        }

        sortAndRemoveEmptyNode(root);
        String json = jsonMapper.toJson(root);
        TreeJson treeJson = new TreeJson(json);
        DataSource.Data data = new DataSource.Data(treeJson, pageMap, mediaMap);
        dataSource.setAllData(data);
        setPublishedData(root, mediaMap);
        logger.info("dataSource: {}", dataSource);
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

    private void setPublishedData(TreeNode root, Map<String, Media> mediaMap) {
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
        pageMap.put("/", homepage);
        removeEmptyDirNode(allDirNodes);
        String json = jsonMapper.toJson(root);
        dataSource.setPublishedData(new DataSource.Data(new TreeJson(json), pageMap, mediaMap));
    }

    private Page newHomepage() {
        String body = "Welcome";
        Date now = new Date();
        Page page = new Page();
        page.setCreated(now);
        page.setModified(now);
        page.setPublished(true);
        page.setBody(body);
        page.setSource(body);
        page.setTitle("Home");
        page.setBodyHtml(body);
        page.setLastModified(now.getTime());
        page.setPath("/");
        return page;
    }
}
