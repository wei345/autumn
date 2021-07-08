package io.liuwei.autumn;

import com.google.common.collect.Maps;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Map;

/**
 * @author liuwei
 * @since 2021-07-07 15:51
 */
@Component
@Slf4j
public class DataFileDao {

    private final String dataDir;

    public DataFileDao(@Value("${autumn.data-dir}") String dataDir) {
        this.dataDir = dataDir;
    }

    public Map<String, File> getAllFileMap() throws IOException {
        File dataDirFile = new File(dataDir);
        IOFileFilter fileFilter = new DataFileFilter();
        Collection<File> files = FileUtils.listFiles(dataDirFile, fileFilter, fileFilter);

        Map<String, File> allMap = Maps.newHashMapWithExpectedSize(files.size());
        String dataDirCanonicalPath = dataDirFile.getCanonicalPath();
        for (File file : files) {
            String relativePath = file.getCanonicalPath().substring(dataDirCanonicalPath.length());
            allMap.put(relativePath, file);
        }
        log.info("found {} files in data dir {}", allMap.size(), dataDir);
        return allMap;
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
