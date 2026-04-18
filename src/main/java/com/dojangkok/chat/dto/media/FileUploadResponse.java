package com.dojangkok.chat.dto.media;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class FileUploadResponse {

    private final List<FileUploadItem> fileItems;

    @Getter
    @Builder
    public static class FileUploadItem {
        private final String fileAssetId;
        private final String presignedUrl;
        private final String fileKey;
    }
}
