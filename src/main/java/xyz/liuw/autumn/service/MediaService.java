package xyz.liuw.autumn.service;

import com.google.common.io.ByteStreams;
import com.vip.vjtools.vjkit.io.IOUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
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


    @Value("${server.compression.mime-types}")
    private List<String> compressionMimeTypes;

    static Object handleRequest(byte[] content,
                                String etag,
                                String filename,
                                String mimeType,
                                WebRequest webRequest,
                                HttpServletRequest request) {
        if (WebUtil.checkNotModified(webRequest, etag)) {
            return null;
        }

        logger.debug("uri={}, content.length={}", request.getRequestURI(), content.length);

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
        String encodedFilename = new String(filename.getBytes(StandardCharsets.UTF_8), StandardCharsets.ISO_8859_1);
        return "attachment; filename=\"" + encodedFilename + "\"";
    }

    public Object handleRequest(Media media,
                                WebRequest webRequest,
                                HttpServletRequest request,
                                HttpServletResponse response) throws IOException {
        File file = media.getFile();

        if (media.getContent() != null) {
            return handleRequest(
                    media.getContent(),
                    WebUtil.getEtag(media.getMd5()),
                    file.getName(),
                    media.getMimeType(),
                    webRequest,
                    request);
        }

        Supplier<InputStream> inputStreamSupplier = () -> {
            try {
                logger.info("Reading big file {} for response output, length={}", file.getAbsolutePath(), file.length());
                return new FileInputStream(file);
            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            }
        };

        handleRequest(
                inputStreamSupplier,
                (int) file.length(),
                WebUtil.getEtag(media.getMd5()),
                file.getName(),
                media.getMimeType(),
                webRequest,
                request,
                response);
        return null;
    }


    private void handleRequest(Supplier<InputStream> inputStreamSupplier,
                               int length,
                               String etag,
                               String filename,
                               String mimeType,
                               WebRequest webRequest,
                               HttpServletRequest request,
                               HttpServletResponse response) throws IOException {
        if (WebUtil.checkNotModified(webRequest, etag)) {
            return;
        }

        logger.debug("uri={}, content length={}", request.getRequestURI(), length);

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
            long copied = ByteStreams.copy(in, output);
            output.flush();
            logger.info("output number of bytes: {}", copied);
        } finally {
            IOUtil.closeQuietly(in);
        }
    }

}