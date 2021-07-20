package io.liuwei.autumn.controller;

import io.liuwei.autumn.enums.AccessLevelEnum;
import io.liuwei.autumn.model.Media;
import io.liuwei.autumn.model.RevisionEtag;
import io.liuwei.autumn.service.MediaService;
import io.liuwei.autumn.util.WebUtil;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * @author liuwei
 * @since 2021-07-20 23:04
 */
@Controller
@RequestMapping
public class MediaController {

    @Autowired
    private MediaService mediaService;

    // 带扩展名，访问文件
    @GetMapping(value = "/**/*.*")
    @ResponseBody
    public ResponseEntity<byte[]> getMedia(AccessLevelEnum accessLevel, HttpServletRequest request,
                                           HttpServletResponse response) throws IOException {
        String path = WebUtil.getInternalPath(request);
        Media media = mediaService.getMedia(path);
        RevisionEtag re;

        if (media == null
                || !media.getAccessLevel().allow(accessLevel)
                || (re = mediaService.getRevisionEtag(media)) == null) {
            response.sendError(404);
            return null;
        }

        if (WebUtil.checkNotModified(re.getRevision(), re.getEtag(), request, response)) {
            return ResponseEntity
                    .status(HttpStatus.NOT_MODIFIED)
                    .eTag(re.getEtag())
                    .build();
        }

        if (re.getRevisionContent() != null) {
            return ResponseEntity
                    .status(HttpStatus.OK)
                    .eTag(re.getEtag())
                    .contentType(re.getRevisionContent().getMediaType())
                    .body(re.getRevisionContent().getContent());
        } else {
            WebUtil.setEtag(re.getEtag(), response);
            response.setContentType(media.getMediaType().toString());
            OutputStream out = response.getOutputStream();
            try (FileInputStream in = new FileInputStream(media.getFile())) {
                IOUtils.copy(in, out);
            }
            out.flush();
            return null;
        }
    }
}
