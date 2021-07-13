package io.liuwei.autumn.model;

import lombok.Data;

/**
 * @author liuwei
 * @since 2021-07-13 23:00
 */
@Data
public class RevisionEtag {
    private final String revision;
    private final String etag;
    private RevisionContent revisionContent;

    public RevisionEtag(String revision, String etag) {
        this.revision = revision;
        this.etag = etag;
    }

    public RevisionEtag(RevisionContent revisionContent) {
        this.revisionContent = revisionContent;
        this.revision = revisionContent.getRevision();
        this.etag = revisionContent.getEtag();
    }
}
