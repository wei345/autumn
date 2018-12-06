package xyz.liuw.autumn.service;

import com.vip.vjtools.vjkit.io.FileUtil;
import com.vip.vjtools.vjkit.io.IOUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.DigestUtils;
import org.springframework.web.context.request.WebRequest;
import xyz.liuw.autumn.data.Media;
import xyz.liuw.autumn.util.MimeTypeUtil;
import xyz.liuw.autumn.util.WebUtil;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.function.Supplier;

@Component
public class MediaService {

    private static Logger logger = LoggerFactory.getLogger(MediaService.class);

    @SuppressWarnings("FieldCanBeLocal")
    private int cacheFileMaxLength = 1024 * 100;

    @Value("${server.compression.mime-types}")
    private List<String> compressionMimeTypes;


    public Object output(Media media, WebRequest webRequest,
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

                    String mimeType = MimeTypeUtil.getMimeType(file.getName());
                    media.setMimeType(mimeType);
                }
            }
        }

        if (media.getContent() != null) {
            return output(media.getContent(),
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
        return null;
    }

    private Object output(byte[] content,
                          int length,
                          String etag,
                          String filename,
                          String mimeType,
                          WebRequest webRequest,
                          HttpServletRequest request,
                          HttpServletResponse response) {
        etag = WebUtil.padEtagIfNecessary(etag);
        if (webRequest.checkNotModified(etag)) {
            return null;
        }

        response.setContentType(mimeType);

        // 设置弹出下载文件请求窗口的 Header
        if (request.getParameter("download") != null) {
            setFileDownloadHeader(response, filename);
            return ResponseEntity.status(HttpStatus.OK)
                    .contentType(MimeTypeUtil.getMediaTypeByMimeType("application/x-msdownload"))
                    .body(content);
        }

        return ResponseEntity
                .status(HttpStatus.OK)
                .contentType(MimeTypeUtil.getMediaTypeByMimeType(mimeType))
                .body(content);
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

        if (!compressionMimeTypes.contains(mimeType)) {
            response.setContentLength(length);
        }
        // 已经启用 Spring Boot gzip，这里不要再 gzip，否则二者冲突 response body 无内容
        OutputStream output = response.getOutputStream();

        InputStream in = null;
        try {
            in = inputStreamSupplier.get();
            IOUtil.copy(in, output);
            output.flush();
        } finally {
            IOUtil.closeQuietly(in);
        }
    }

    /**
     * 设置让浏览器弹出下载对话框的 Header.
     *
     * @param fileName 下载后的文件名.
     */
    private void setFileDownloadHeader(HttpServletResponse response, String fileName) {
        // 中文文件名支持
        String encodedfileName = new String(fileName.getBytes(StandardCharsets.UTF_8), StandardCharsets.ISO_8859_1);
        response.setContentType("application/x-msdownload");
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + encodedfileName + "\"");
    }


}