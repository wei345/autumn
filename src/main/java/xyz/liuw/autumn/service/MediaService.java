package xyz.liuw.autumn.service;

import com.vip.vjtools.vjkit.io.FileUtil;
import com.vip.vjtools.vjkit.io.IOUtil;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.DigestUtils;
import org.springframework.web.context.request.WebRequest;
import xyz.liuw.autumn.data.Media;
import xyz.liuw.autumn.util.WebUtil;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.function.Supplier;

@Component
public class MediaService {

    private static Logger logger = LoggerFactory.getLogger(MediaService.class);

    @SuppressWarnings("FieldCanBeLocal")
    private int cacheFileMaxLength = 1024 * 100;

    /**
     * 设置让浏览器弹出下载对话框的 Header.
     *
     * @param fileName 下载后的文件名.
     */
    private static void setFileDownloadHeader(HttpServletResponse response, String fileName) {
        // 中文文件名支持
        String encodedfileName = new String(fileName.getBytes(StandardCharsets.UTF_8), StandardCharsets.ISO_8859_1);
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + encodedfileName + "\"");
    }

    public void output(Media media, WebRequest webRequest,
                       HttpServletRequest request, HttpServletResponse response) throws IOException {
        File file = media.getFile();
        // 设置 md5 和 mimeType，如果文件不大，还会缓存内容
        if (media.getMd5() == null) {
            //noinspection SynchronizationOnLocalVariableOrMethodParameter
            synchronized (media) {
                if (media.getMd5() == null) {
                    if (file.length() <= cacheFileMaxLength) {
                        logger.info("Caching small file content and calculate md5 {}", file.getAbsolutePath());
                        media.setContent(FileUtil.toByteArray(file));
                        media.setMd5(DigestUtils.md5DigestAsHex(media.getContent()));
                    } else {
                        // md5
                        logger.info("Calculating big file md5 {}", file.getAbsolutePath());
                        InputStream in = new FileInputStream(file);
                        media.setMd5(DigestUtils.md5DigestAsHex(in));
                        IOUtil.closeQuietly(in);
                    }

                    String mimeType = MediaTypeFactory
                            .getMediaType(file.getName())
                            .orElse(MediaType.APPLICATION_OCTET_STREAM)
                            .toString();
                    media.setMimeType(mimeType);
                }
            }
        }

        if (media.getContent() != null) {
            output(() -> new ByteArrayInputStream(media.getContent()),
                    media.getContent().length,
                    media.getMd5(),
                    media.getFile().getName(),
                    media.getMimeType(),
                    webRequest, request, response);
        } else {
            output(() -> {
                        try {
                            logger.info("Reading big file for response output {}", file.getAbsolutePath());
                            return new FileInputStream(file);
                        } catch (FileNotFoundException e) {
                            throw new RuntimeException(e);
                        }
                    },
                    (int) file.length(),
                    media.getMd5(),
                    file.getName(),
                    media.getMimeType(),
                    webRequest, request, response);
        }
    }

    private void output(Supplier<InputStream> inputStreamSupplier,
                        int length,
                        String etag,
                        String filename,
                        String mimeType,
                        WebRequest webRequest,
                        HttpServletRequest request,
                        HttpServletResponse response) throws IOException {
        etag = WebUtil.padEtagIfNecessary(etag);
        if (webRequest.checkNotModified(etag)) {
            return;
        }

        response.setContentType(mimeType);

        // 设置弹出下载文件请求窗口的 Header
        if (request.getParameter("download") != null) {
            setFileDownloadHeader(response, filename);
        }

        response.setContentLength(length);
        // 已经启用 Spring Boot gzip，这里不要再 gzip，否则二者冲突 response body 无内容
        OutputStream output = response.getOutputStream();

        InputStream in = null;
        try {
            in = inputStreamSupplier.get();
            IOUtil.copy(in, output);
        } finally {
            IOUtil.closeQuietly(in);
        }
        output.flush();
    }

}