package io.liuwei.autumn;

import com.google.common.collect.Maps;
import io.liuwei.autumn.enums.SourceFormatEnum;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * @author liuwei
 * @since 2021-07-07 15:51
 */
@Component
@Slf4j
public class DataFileDao {

    private final String dataDir;

    // file path -> File. file path 以 "/" 开头，"/" 表示数据目录
    private Map<String, File> allFileMap;
    private Map<String, File> articleMap;

    public DataFileDao(@Value("${autumn.data-dir}") String dataDir) {
        this.dataDir = dataDir;
    }

    @PostConstruct
    public void init() throws IOException {
        File dataDirFile = new File(dataDir);
        IOFileFilter fileFilter = new DataFileFilter();
        Collection<File> files = FileUtils.listFiles(dataDirFile, fileFilter, fileFilter);

        Map<String, File> allMap = Maps.newHashMapWithExpectedSize(files.size());
        Map<String, File> articleMap = Maps.newHashMapWithExpectedSize(files.size());
        String dataDirCanonicalPath = dataDirFile.getCanonicalPath();
        for (File file : files) {
            String relativePath = file.getCanonicalPath().substring(dataDirCanonicalPath.length());
            allMap.put(relativePath, file);
            if (SourceFormatEnum.getByFileName(file.getName()) == SourceFormatEnum.ASCIIDOC) {
                String articlePath = StringUtils.substringBeforeLast(relativePath, ".");
                articleMap.put(articlePath, file);
            }
        }
        this.allFileMap = allMap;
        this.articleMap = articleMap;
        log.info("found {} files in data dir {}", allMap.size(), dataDir);
    }

    public File getByPath(String path) {
        return allFileMap.get(path);
    }

    public File getArticleFileByPath(String path) {
        return articleMap.get(path);
    }

    public List<File> getAllFiles() {
        return new ArrayList<>(allFileMap.values());
    }

    public List<File> getAllArticleFiles() {
        return new ArrayList<>(articleMap.values());
    }

    private static class DataFileFilter implements IOFileFilter {
        @Override
        public boolean accept(File file) {
            if (file.isHidden() ||
                    file.getName().startsWith(".") ||
                    file.getName().startsWith("_") ||
                    file.getName().endsWith(".iml")) {
                return false;
            }
            return true;
        }

        @Override
        public boolean accept(File dir, String name) {
            if (dir.isHidden() ||
                    dir.getName().startsWith(".") ||
                    dir.getName().startsWith("_") ||
                    dir.getName().toLowerCase().endsWith(".graffle") ||
                    dir.getName().equalsIgnoreCase("target")) {
                return false;
            }
            return true;
        }
    }

}
