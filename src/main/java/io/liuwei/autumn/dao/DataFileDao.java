package io.liuwei.autumn.dao;

import com.google.common.base.Functions;
import com.google.common.io.Files;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author liuwei
 * @since 2021-07-07 15:51
 */
@SuppressWarnings("UnstableApiUsage")
@Component
@Slf4j
public class DataFileDao {

    @Getter
    private final String dataDir;

    private final Set<String> excludes;

    public DataFileDao(@Value("${autumn.data.dir}") String dataDir,
                       @Value("${autumn.data.excludes}") List<String> excludes) {
        this.dataDir = Files.simplifyPath(dataDir);
        this.excludes = excludes == null ?
                Collections.emptySet() :
                excludes.stream().map(Files::simplifyPath).collect(Collectors.toSet());
    }

    public Map<String, File> getAllFileMap() {
        File dataDirFile = new File(dataDir);
        IOFileFilter fileFilter = new DataFileFilter();
        return FileUtils
                .listFiles(dataDirFile, fileFilter, fileFilter)
                .stream()
                .collect(Collectors.toMap(this::toRelativePath, Functions.identity()));
    }

    private String toRelativePath(File file) {
        return file.getPath().substring(dataDir.length());
    }

    private class DataFileFilter implements IOFileFilter {
        @Override
        public boolean accept(File file) {
            if (file.isHidden() ||
                    file.getName().startsWith(".") ||
                    file.getName().startsWith("_") ||
                    excludes.contains(toRelativePath(file))) {
                return false;
            }

            if (file.isFile()) {
                return !file.getName().endsWith(".iml");
            } else {
                return !file.getName().toLowerCase().endsWith(".graffle") &&
                        !file.getName().equalsIgnoreCase("target");
            }
        }

        @Override
        public boolean accept(File dir, String name) {
            throw new IllegalStateException("不会调用这个方法");
        }
    }

}
