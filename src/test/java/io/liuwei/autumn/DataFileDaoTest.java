package io.liuwei.autumn;

import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.Map;

/**
 * @author liuwei
 * @since 2021-07-09 13:55
 */
public class DataFileDaoTest {

    @Test
    public void getAllFileMap() throws IOException {
        DataFileDao dao = new DataFileDao("../notes");
        Map<String, File> fileMap =  dao.getAllFileMap();
        System.out.println(fileMap.get("/favicon.ico"));
    }
}