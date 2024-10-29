package io.liuwei.autumn.controller;

import io.liuwei.autumn.config.AppProperties;
import io.liuwei.autumn.enums.AccessLevelEnum;
import io.liuwei.autumn.util.Md5Util;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * @author liuwei
 * @since 2024-10-29 14:00
 */
@Controller
@RequestMapping(ClipboardController.PATH)
@RequiredArgsConstructor
public class ClipboardController {

    static final String PATH = "/clipboard";

    private final MediaType TEXT_PLAIN_UTF_8 = new MediaType(MediaType.TEXT_PLAIN,
            StandardCharsets.UTF_8);

    private final StringRedisTemplate stringRedisTemplate;

    private final AppProperties appProperties;

    @GetMapping
    public String write(AccessLevelEnum accessLevel,
                        HttpServletResponse response) throws IOException {
        if (!AccessLevelEnum.USER.allow(accessLevel)) {
            response.sendError(404);
            return null;
        }
        return "clipboard";
    }

    @PostMapping
    public String submit(@RequestParam(required = false) String content,
                         AccessLevelEnum accessLevel,
                         HttpServletResponse response) throws IOException {
        if (!AccessLevelEnum.USER.allow(accessLevel)) {
            response.sendError(404);
            return null;
        }

        if (StringUtils.isBlank(content))
            return "redirect:" + PATH;
        String id = getContentId(content);
        Duration maxAge = appProperties.getClipboard().getMaxAge();
        String key = getRedisKeyOfContentId(id);
        stringRedisTemplate.opsForValue().set(key, content, maxAge);
        return "redirect:" + PATH + "/" + id;
    }

    String getContentId(String content) {
        byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
        String md5 = Md5Util.md5DigestAsHex(bytes);
        int digestLength = Math.min(appProperties.getClipboard()
                .getDigestLength(), md5.length());
        return md5.substring(0, digestLength);
    }

    String getRedisKeyOfContentId(String contentId) {
        String namespace = appProperties.getClipboard().getNamespaceForRedisKeys();
        return String.format("%s:%s", namespace, contentId);
    }

    @GetMapping("{id}")
    public ResponseEntity<String> read(@PathVariable String id) {
        if (StringUtils.isNotBlank(id)) {
            String key = getRedisKeyOfContentId(id);
            String value = stringRedisTemplate.opsForValue().get(key);
            if (value != null)
                return ResponseEntity.ok()
                        .contentType(TEXT_PLAIN_UTF_8)
                        .body(value);
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body("Page Not Found");
    }

    // todo save content time, size
    // todo list all ids
    // todo delete content

}
