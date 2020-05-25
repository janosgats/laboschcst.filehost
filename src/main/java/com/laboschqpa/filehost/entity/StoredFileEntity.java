package com.laboschqpa.filehost.entity;

import com.laboschqpa.filehost.enums.IndexedFileStatus;
import lombok.*;

import javax.persistence.*;
import java.time.Instant;


@Data
@NoArgsConstructor
@ToString(callSuper = true)
@Entity
@Table(name = "stored_file",
        indexes = {
                @Index(columnList = "size", name = "size")
        }
)
@DiscriminatorValue("1")
public class StoredFileEntity extends IndexedFileEntity {
    @Builder
    public StoredFileEntity(IndexedFileStatus status, Long ownerUserId, Long ownerTeamId, final String path, final Long size, final Instant creationTime,
                            final String eTag, final String originalFileName) {
        super(status, ownerUserId, ownerTeamId, creationTime);
        this.path = path;
        this.size = size;
        this.eTag = eTag;
        this.originalFileName = originalFileName;
    }

    @Column(name = "path")
    private String path;

    @JoinColumn(name = "size")
    private Long size;//Size in Bytes

    @Column(name = "e_tag")
    private String eTag;

    @Column(name = "original_file_name")
    private String originalFileName;
}
