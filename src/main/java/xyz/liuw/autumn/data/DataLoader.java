package xyz.liuw.autumn.data;

import ch.qos.logback.classic.Level;
import com.google.common.collect.Maps;
import com.vip.vjtools.vjkit.mapper.JsonMapper;
import com.vip.vjtools.vjkit.number.MathUtil;
import com.vip.vjtools.vjkit.text.StringBuilderHolder;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import xyz.liuw.autumn.util.WebUtil;

import javax.annotation.PostConstruct;
import javax.validation.constraints.NotNull;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static org.springframework.web.util.HtmlUtils.htmlEscape;

/**
 * @author liuwei
 * Created by liuwei on 2018/11/19.
 */
@Component
public class DataLoader {

    private static final String ARCHIVE_PATH_PREFIX = "/archive/";

    private static final String HELP_MD = "/static/help.md";

    private static Logger logger = LoggerFactory.getLogger(DataLoader.class);

    private final DataSource dataSource;

    private final JsonMapper jsonMapper;

    @Value("${autumn.data-dir}")
    private String dataDir;

    @Value("${autumn.data.reload-interval-seconds:10}")
    private long reloadIntervalSeconds;

    @Value("${autumn.data.publish-all-media:true}")
    private boolean publishAllMedia;

    private int reloadContinuousFailures; // default 0

    @Autowired
    private WebUtil webUtil;

    private List<DataChangedListener> listeners = new ArrayList<>(1);

    @Autowired
    private ResourceLoader resourceLoader;

    private volatile Page helpPage;

    @Autowired
    public DataLoader(DataSource dataSource, JsonMapper jsonMapper) {
        this.dataSource = dataSource;
        this.jsonMapper = jsonMapper;
    }

    @PostConstruct
    void start() {
        initHelpPage();

        // load
        ch.qos.logback.classic.Logger parserLogger = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(PageParser.class);
        Level oldLevel = parserLogger.getLevel();
        if (oldLevel == null || oldLevel == Level.INFO) {
            logger.debug("已将 PageParser 日志级别设为 WARN，初始加载完成后会恢复，这是为避免每次启动输出 1000 多条解析日志");
            logger.debug("可设置 logging.level.{}=DEBUG 来阻止这个自动修改日志级别的行为", PageParser.class.getName());
            parserLogger.setLevel(Level.WARN);
        }
        logger.info("Data loading: {}", dataDir);
        long start = System.currentTimeMillis();
        load();
        logger.info("Data loaded in {} ms", System.currentTimeMillis() - start);
        if (oldLevel == null || oldLevel == Level.INFO) {
            parserLogger.setLevel(oldLevel);
            logger.debug("已恢复 PageParser 日志级别 {}", oldLevel);
        }

        timingReload();
    }

    private void timingReload() {
        if (reloadIntervalSeconds <= 0) {
            return;
        }

        String threadName = getClass().getSimpleName() + ".timingReload";
        Thread thread = new Thread(() -> {
            logger.info("Started thread '{}'", threadName);
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
        }, threadName);
        thread.setDaemon(true);
        thread.start();
    }

    @SuppressWarnings("WeakerAccess")
    public synchronized void load() {
        Validate.notBlank(dataDir, "config 'autumn.data-dir' is empty");
        load(new File(dataDir));
    }

    private void load(File rootDir) {
        Validate.notNull(rootDir, "rootDir is null");
        Validate.isTrue(!rootDir.isHidden(), "rootDir is hidden");
        Validate.isTrue(rootDir.isDirectory(), "rootDir is not a directory");

        Map<String, Page> oldPageMap = dataSource.getAllData().getPageMap();
        Map<String, Page> pageMap = Maps.newHashMapWithExpectedSize(
                oldPageMap.size() == 0 ? 1500 : oldPageMap.size());
        int pageAddedOrModified = 0;

        Map<String, Media> oldMediaMap = dataSource.getAllData().getMediaMap();
        Map<String, Media> mediaMap = Maps.newHashMapWithExpectedSize(
                oldMediaMap.size() == 0 ? 100 : oldMediaMap.size());
        int mediaAddedOrModified = 0;

        TreeNode root = new TreeNode("home", "/", true);
        Stack<File> dirStack = new Stack<>();
        Stack<TreeNode> nodeStack = new Stack<>();
        dirStack.push(rootDir);
        nodeStack.push(root);
        while (!dirStack.isEmpty()) {
            File dir = dirStack.pop();
            TreeNode parent = nodeStack.pop();

            //noinspection ConstantConditions
            for (File file : dir.listFiles()) {
                if (file.isHidden() || file.getName().startsWith(".")) {
                    continue;
                }
                if (file.isDirectory() && file.getName().endsWith(".mindnode")) {
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
                    // Page
                    if (file.getName().endsWith(".md")) {
                        String name = filename(file);
                        String path = parent.path + name;

                        // parse page
                        if ("/index".equals(path) || "/sidebar".equals(path)) {
                            // skip dokuwiki page
                            continue;
                        }
                        Page page = oldPageMap.get(path);
                        if (page == null || page.getLastModified() != file.lastModified()) {
                            pageAddedOrModified++;
                            page = PageParser.parse(file);
                            page.setPath(path);
                            page.setArchived(path.startsWith(ARCHIVE_PATH_PREFIX));
                        }
                        pageMap.put(path, page);

                        // add node
                        TreeNode node = new TreeNode(name, path, false);
                        node.setPage(page);
                        parent.addChild(node);
                    }
                    // Media
                    else {
                        String path = parent.path + file.getName();
                        Media media = oldMediaMap.get(path);
                        if (media == null || media.getLastModified() != file.lastModified()) {
                            mediaAddedOrModified++;
                            media = new Media(file);
                        }
                        mediaMap.put(path, media);
                    }
                }
            }
        }

        boolean pageChanged = pageAddedOrModified > 0 || pageMap.size() != oldPageMap.size();
        boolean mediaChanged = mediaAddedOrModified > 0 || mediaMap.size() != oldMediaMap.size();
        if (!pageChanged && !mediaChanged) {
            logger.debug("dataSource no change");
            return;
        }

        logger.info("Page added or modified: {}, Media added or modified: {}", pageAddedOrModified, mediaAddedOrModified);
        sortAndRemoveEmptyNode(root);
        String json = jsonMapper.toJson(root);
        TreeJson treeJson = new TreeJson(json);
        DataSource.Data data = new DataSource.Data(
                treeJson,
                newHomepage(pageMap, false),
                pageMap,
                mediaMap);
        data.setHelpPage(helpPage);
        dataSource.setAllData(data);
        setPublishedData(root, mediaMap);
        logger.info("dataSource: {}", dataSource);
        listeners.forEach(DataChangedListener::onChanged);
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
                } else if (nd.getPage().isPublished()) {
                    pageMap.put(nd.getPath(), nd.getPage());
                    publishedList.add(nd);
                }
            }
            node.children = publishedList;
        }
        removeEmptyDirNode(allDirNodes);
        String json = jsonMapper.toJson(root);

        // published media
        Map<String, Media> publishedMedia = Maps.newHashMapWithExpectedSize(mediaMap.size());
        for (Map.Entry<String, Media> entry : mediaMap.entrySet()) {
            if (publishAllMedia) {
                publishedMedia.put(entry.getKey(), entry.getValue());
            } else {
                String path = entry.getKey();
                if (path.endsWith(".png") || path.endsWith(".jpg") || path.endsWith(".ico")) {
                    publishedMedia.put(entry.getKey(), entry.getValue());
                }
            }
        }

        DataSource.Data data = new DataSource.Data(
                new TreeJson(json),
                newHomepage(pageMap, true),
                pageMap,
                publishedMedia);
        data.setHelpPage(helpPage);
        dataSource.setPublishedData(data);
    }

    private Page newHomepage(@Nullable Map<String, Page> pageMap, boolean published) {

        String title = "Home";
        String body = "Welcome";
        if (!CollectionUtils.isEmpty(pageMap)) {
            List<Page> recently = getListOrderByModifiedDesc(pageMap.values());
            if (!CollectionUtils.isEmpty(recently)) {
                StringBuilder stringBuilder = StringBuilderHolder.getGlobal();
                stringBuilder.append("## Recently Modified\n")
                        .append("<ul class='recently_modified'>");
                for (int i = 0; i < 20 && i < recently.size(); i++) {
                    Page page = recently.get(i);

                    stringBuilder.append("<li><a href='")
                            .append(webUtil.getContextPath())
                            .append(page.getPath())
                            .append("'>")
                            .append(htmlEscape(page.getTitle()))
                            .append(" <i data-time=\"")
                            .append(page.getModified().getTime())
                            .append("\"")
                            .append(">")
                            .append(String.format("%tF", page.getModified().getTime()))
                            .append("</i></a></li>")
                            .append("\n");
                }
                stringBuilder.append("</ul>");
                title = "Home";
                body = stringBuilder.toString();
            }
        }

        Date now = new Date(0); // 不要让 Home 出现在最近修改列表里
        Page page = new Page();
        page.setCreated(now);
        page.setModified(now);
        page.setPublished(published);
        page.setBody(body);
        page.setSource(body);
        page.setTitle(title);
        page.setLastModified(now.getTime());
        page.setPath("/");
        return page;
    }

    private List<Page> getListOrderByModifiedDesc(@NotNull Collection<Page> set) {
        List<Page> list = new ArrayList<>(set);
        list.sort((o1, o2) -> {
            // 一定要分出先后，也就是不能返回 0，否则每次搜索结果顺序可能不完全一样
            int v;

            // 非归档目录
            v = Integer.compare(o1.getPath().startsWith(ARCHIVE_PATH_PREFIX) ? 1 : 0,
                    o2.getPath().startsWith(ARCHIVE_PATH_PREFIX) ? 1 : 0);
            if (v != 0) {
                return v;
            }

            // 最近修改日期
            v = Long.compare(o2.getModified().getTime(), o1.getModified().getTime());
            if (v != 0) {
                return v;
            }

            // 字典顺序
            return o1.getPath().compareTo(o2.getPath());
        });
        return list;
    }

    public void addListener(DataChangedListener listener) {
        this.listeners.add(listener);
    }

    private void initHelpPage() {
        setHelpPage(resourceLoader.getResourceCache(HELP_MD));

        resourceLoader.addStaticChangedListener(() -> {
            ResourceLoader.ResourceCache h = resourceLoader.getResourceCache(HELP_MD);
            if (h.getLastModified() > helpPage.getLastModified()) {
                setHelpPage(h);
            }
        });
    }

    private void setHelpPage(ResourceLoader.ResourceCache resourceCache) {
        Page page = newPageOf(resourceCache);
        page.setPath("/help");
        helpPage = page;
        dataSource.getAllData().setHelpPage(helpPage);
        dataSource.getPublishedData().setHelpPage(helpPage);
        logger.info("/help updated");
    }

    private Page newPageOf(ResourceLoader.ResourceCache resourceCache) {
        String content = new String(resourceCache.getContent(), StandardCharsets.UTF_8);
        Page page = PageParser.parse(content);
        Date date = new Date(resourceCache.getLastModified());
        page.setCreated(date);
        page.setModified(date);
        page.setLastModified(date.getTime());
        return page;
    }

    public interface DataChangedListener {
        void onChanged();
    }
}
