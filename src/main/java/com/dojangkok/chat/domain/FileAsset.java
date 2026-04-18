package com.dojangkok.chat.domain;

import lombok.Builder;
import lombok.Getter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Getter
@Document(collection = "file_assets")
public class FileAsset {

    @Id
    private String id;

    @Indexed(unique = true)
    private String fileKey;
    private String roomId;
    private String uploaderId;
    private FileAssetStatus status;
    private String contentType;
    private String originalFilename;
    private Long fileSize;
    private Instant createdAt;
    private Instant completedAt;

    @Builder
    public FileAsset(String fileKey, String roomId, String uploaderId,
                     String contentType, String originalFilename) {
        this.fileKey = fileKey;
        this.roomId = roomId;
        this.uploaderId = uploaderId;
        this.contentType = contentType;
        this.originalFilename = originalFilename;
        this.status = FileAssetStatus.UPLOADING;
        this.createdAt = Instant.now();
    }

    public void markCompleted(long fileSize) {
        this.status = FileAssetStatus.COMPLETED;
        this.fileSize = fileSize;
        this.completedAt = Instant.now();
    }

    public void markExpired() {
        this.status = FileAssetStatus.EXPIRED;
    }

    public enum FileAssetStatus {
        UPLOADING, COMPLETED, EXPIRED
    }
}
