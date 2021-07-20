package io.liuwei.autumn.service;

import io.liuwei.autumn.manager.MediaManager;
import io.liuwei.autumn.manager.RevisionContentManager;
import io.liuwei.autumn.model.Media;
import io.liuwei.autumn.model.RevisionEtag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author liuwei
 * @since 2021-07-20 23:02
 */
@Component
@Slf4j
public class MediaService {
    @Autowired
    private MediaManager mediaManager;

    @Autowired
    private RevisionContentManager revisionContentManager;

    public Media getMedia(String path) {
        return mediaManager.getMedia(path);
    }

    public RevisionEtag getRevisionEtag(Media media) {
        return revisionContentManager.getRevisionEtag(media);
    }

}
