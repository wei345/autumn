package xyz.liuw.autumn.service;

import com.vip.vjtools.vjkit.io.FileUtil;
import com.vip.vjtools.vjkit.io.IOUtil;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.util.DigestUtils;
import org.springframework.web.context.request.WebRequest;
import xyz.liuw.autumn.data.Media;

import javax.activation.MimetypesFileTypeMap;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.zip.GZIPOutputStream;

@Component
public class ContentService {

    /**
     * 需要被Gzip压缩的Mime类型.
     */
    private static final String[] GZIP_MIME_TYPES = {"text/html", "application/xhtml+xml", "text/plain", "text/css",
            "text/javascript", "application/x-javascript", "application/json"};
    /**
     * 需要被Gzip压缩的最小文件大小.
     */
    private static final int GZIP_MINI_LENGTH = 512;
    private static Logger logger = LoggerFactory.getLogger(ContentService.class);
    private MimetypesFileTypeMap mimetypesFileTypeMap;

    private int cacheFileMaxLength = 1024 * 100;

    public ContentService() {
        // 初始化mimeTypes, 默认缺少css的定义,添加之.
        mimetypesFileTypeMap = new MimetypesFileTypeMap();
        mimetypesFileTypeMap.addMimeTypes("text/css css");
    }

    public void output(Media media, WebRequest webRequest,
                       HttpServletRequest request, HttpServletResponse response) throws IOException {
        File file = media.getFile();
        // 设置 md5 和 mimeType，如果文件不大，还会缓存内容
        if (media.getMd5() == null) {
            //noinspection SynchronizationOnLocalVariableOrMethodParameter
            synchronized (media) {
                if (media.getMd5() == null) {
                    logger.info("Reading small file {}", file.getAbsolutePath());
                    if (file.length() <= cacheFileMaxLength) {
                        media.setContent(FileUtil.toByteArray(file));
                        media.setMd5(DigestUtils.md5DigestAsHex(media.getContent()));
                    } else {
                        // md5
                        InputStream in = new FileInputStream(file);
                        media.setMd5(DigestUtils.md5DigestAsHex(in));
                        IOUtil.closeQuietly(in);
                    }
                    media.setMimeType(mimetypesFileTypeMap.getContentType(file));
                }
            }
        }

        if (media.getContent() != null) {
            output(new ByteArrayInputStream(media.getContent()),
                    media.getContent().length,
                    media.getMd5(),
                    media.getFile().getName(),
                    media.getMimeType(),
                    webRequest, request, response);
        } else {
            logger.info("Reading big file {}", file.getAbsolutePath());
            InputStream in = null;
            try {
                in = new FileInputStream(file);
                output(in,
                        (int) file.length(),
                        media.getMd5(),
                        file.getName(),
                        media.getMimeType(),
                        webRequest, request, response);
            } finally {
                IOUtil.closeQuietly(in);
            }
        }
    }

    public void output(InputStream in, int length, String etag, String filename, String mimeType, WebRequest webRequest,
                       HttpServletRequest request, HttpServletResponse response) throws IOException {
        etag = padEtagIfNecessary(etag);
        if (webRequest.checkNotModified(etag)) {
            return;
        }

        response.setContentType(mimeType);

        // 设置弹出下载文件请求窗口的 Header
        if (request.getParameter("download") != null) {
            setFileDownloadHeader(response, filename);
        }

        boolean needGzip = (length >= GZIP_MINI_LENGTH) && ArrayUtils.contains(GZIP_MIME_TYPES, mimeType);

        // 构造OutputStream
        OutputStream output;
        if (checkAccetptGzip(request) && needGzip) {
            // 使用压缩传输的 outputstream, 使用 http1.1 trunked 编码不设置 content-length.
            output = buildGzipOutputStream(response);
        } else {
            // 使用普通 outputstream, 设置content-length.
            response.setContentLength(length);
            output = response.getOutputStream();
        }

        IOUtil.copy(in, output);
        output.flush();
    }

    /**
     * 设置让浏览器弹出下载对话框的Header.
     *
     * @param fileName 下载后的文件名.
     */
    private static void setFileDownloadHeader(HttpServletResponse response, String fileName) {
        // 中文文件名支持
        String encodedfileName = new String(fileName.getBytes(StandardCharsets.UTF_8), StandardCharsets.ISO_8859_1);
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + encodedfileName + "\"");

    }

    private String padEtagIfNecessary(String etag) {
        if (StringUtils.isBlank(etag)) {
            return etag;
        }
        if ((etag.startsWith("\"") || etag.startsWith("W/\"")) && etag.endsWith("\"")) {
            return etag;
        }
        return "\"" + etag + "\"";
    }

    /**
     * 检查浏览器客户端是否支持gzip编码.
     */
    private static boolean checkAccetptGzip(HttpServletRequest request) {
        // Http1.1 header
        String acceptEncoding = request.getHeader("Accept-Encoding");

        return StringUtils.contains(acceptEncoding, "gzip");
    }

    /**
     * 设置Gzip Header并返回GZIPOutputStream.
     */
    private OutputStream buildGzipOutputStream(HttpServletResponse response) throws IOException {
        response.setHeader("Content-Encoding", "gzip");
        response.setHeader("Vary", "Accept-Encoding");
        return new GZIPOutputStream(response.getOutputStream());
    }
}