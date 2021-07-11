package io.liuwei.autumn.reader;

/*import ch.qos.logback.classic.Level;
import io.liuwei.autumn.domain.Page;
import io.liuwei.autumn.util.CommandExecutor;
import io.liuwei.autumn.util.ResourceWalker;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.ParseException;
import java.util.Date;

import static java.nio.file.FileVisitResult.*;*/

/**
 * @author liuwei
 * Created by liuwei on 2018/12/18.
 */
@SuppressWarnings("FieldCanBeLocal")
public class PageCreatedRepairer {

    /* 一些依赖类已删除，例如 MarkdownPageReader, PageReaders
    private static final Logger logger = LoggerFactory.getLogger(PageCreatedRepairer.class);
    private static final String dataDir = "";

    static {
        ((ch.qos.logback.classic.Logger) logger).setLevel(Level.INFO);
    }

    public static void main(String[] args) throws IOException {
        PageCreatedRepairer repairer = new PageCreatedRepairer();
        repairer.repairDirectory(new File(dataDir));
    }

    private static void repairFile(File file) {
        Page page = PageReaders.getPageReader(file.getName()).toPage(file, null);
        if (page.getSource().contains("\n~~NOCACHE~~\n")
                && (file.getName().equals("index.md") || file.getName().equals("sidebar.md"))) {
            logger.info("skip dokuwiki page {}", file.getAbsolutePath());
            return;
        }
        Date date = getFirstCommitTimeOf(file);
        if (page.isBlog() && page.getBlogDate().getTime() < date.getTime()) {
            date = page.getBlogDate();
        }
        if (date.getTime() < page.getCreated().getTime()) {
            logger.info("Replace '{}' with '{}'",
                    MarkdownPageReader.DATE_PARSER_ON_SECOND.format(page.getCreated()),
                    MarkdownPageReader.DATE_PARSER_ON_SECOND.format(date));
            page.setCreated(date);
            MarkdownPageWriter.write(page, file);
        }
    }

    private static Date getFirstCommitTimeOf(File file) {
        Validate.isTrue(file.exists(), file.getAbsolutePath() + " not exist");

        String[] commands = new String[]{
                "git",
                "log",
                "--follow",
                "--format=%ad",
                "--date=format:%Y-%m-%d %H:%M:%S",
                file.getAbsolutePath()
        };
        logger.debug(StringUtils.join(commands, " "));

        CommandExecutor.Result result = CommandExecutor.execute(commands);
        if (result.hasError()) {
            throw new RuntimeException(result.getStderr());
        }
        String out = result.getStdout();
        String[] lines = out.split("\n");
        try {
            return MarkdownPageReader.DATE_PARSER_ON_SECOND.parse(lines[lines.length - 1]);
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }


    private void repairDirectory(File directory) throws IOException {

        RepairCreatedVisitor visitor = new RepairCreatedVisitor();

        Files.walkFileTree(Paths.get(directory.toURI()), visitor);

        logger.info("count: {}", visitor.getCount());
    }

    static class RepairCreatedVisitor extends ResourceWalker.SkipHiddenFileVisitor {

        private int count;

        @Override
        public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
            if (super.preVisitDirectory(dir, attrs) == SKIP_SUBTREE) {
                return SKIP_SUBTREE;
            }

            if (dir.getFileName().toString().endsWith(".mindnode")) {
                return SKIP_SUBTREE;
            }

            return CONTINUE;
        }

        @Override
        public FileVisitResult visitNonHiddenFile(Path file, BasicFileAttributes attrs) {

            if (file.getFileName().toString().endsWith(".md")) {
                logger.info("Repairing {}", file);
                repairFile(file.toFile());
                count++;
            }

            return FileVisitResult.CONTINUE;
        }

        int getCount() {
            return count;
        }
    }*/
}
