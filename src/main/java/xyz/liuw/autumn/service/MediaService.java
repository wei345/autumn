package xyz.liuw.autumn.service;

import com.vip.vjtools.vjkit.io.FileUtil;
import com.vip.vjtools.vjkit.io.IOUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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
    private int cacheFileMaxLength = 1024 * 1024; // 1 MB

    @Value("${server.compression.mime-types}")
    private List<String> compressionMimeTypes;

    @Autowired
    private WebUtil webUtil;

    static Object output(byte[] content,
                         String etag,
                         String filename,
                         String mimeType,
                         WebRequest webRequest,
                         HttpServletRequest request,
                         HttpServletResponse response) {
        if (webRequest.checkNotModified(etag)) {
            return null;
        }

        if (request.getParameter("download") != null) {
            mimeType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
        }

        MediaType mediaType = MimeTypeUtil.getMediaTypeByMimeType(mimeType);
        ResponseEntity.BodyBuilder builder = ResponseEntity.status(HttpStatus.OK).contentType(mediaType);
        if (MediaType.APPLICATION_OCTET_STREAM_VALUE.equals(mimeType)) {
            builder.header(HttpHeaders.CONTENT_DISPOSITION, getContentDisposition(filename));
        }
        return builder.body(content);
    }

    private static String getContentDisposition(String filename) {
        // 中文文件名支持
        String encodedFileName = new String(filename.getBytes(StandardCharsets.UTF_8), StandardCharsets.ISO_8859_1);
        return "attachment; filename=\"" + encodedFileName + "\"";
    }

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
                    webUtil.getEtag(media.getMd5()),
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

    private void output(Supplier<InputStream> inputStreamSupplier,
                        int length,
                        String md5,
                        String filename,
                        String mimeType,
                        WebRequest webRequest,
                        HttpServletRequest request,
                        HttpServletResponse response) throws IOException {
        String etag = webUtil.getEtag(md5);
        if (webRequest.checkNotModified(etag)) {
            return;
        }

        if (request.getParameter("download") != null) {
            mimeType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
        }
        response.setContentType(mimeType);
        if (mimeType.equals(MediaType.APPLICATION_OCTET_STREAM_VALUE)) {
            response.addHeader(HttpHeaders.CONTENT_DISPOSITION, getContentDisposition(filename));
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

}